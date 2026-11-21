package com.panita.enriquecraft.client.share;

import com.mojang.blaze3d.platform.NativeImage;
import com.panita.enriquecraft.discord.service.ChatImageLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shows the screenshot the player picked, lets them add a message, and says once what it may show. Nothing is sent until the
 * share button is pressed; Escape and Cancel send nothing. The copy button only puts the picture on the clipboard.
 */
final class ShareScreen extends Screen {

    private static final Identifier PREVIEW = Identifier.fromNamespaceAndPath("enriquecraft", "share_preview");
    private static final int BUTTON_WIDTH = 90;
    private static final int GAP = 6;
    private static final int WARNING_COLOR = 0xFFF2B134;

    private final Path file;
    private DynamicTexture texture;
    private EditBox caption;

    ShareScreen(Path file) {
        super(Component.translatable("enriquecraft.share.title"));
        this.file = file;
    }

    @Override
    protected void init() {
        if (texture == null) {
            try (InputStream input = Files.newInputStream(file)) {
                texture = new DynamicTexture(() -> "enriquecraft share preview", NativeImage.read(input));
                minecraft.getTextureManager().register(PREVIEW, texture);
            } catch (IOException e) {
                onClose();
                return;
            }
        }
        int boxWidth = Math.min(width - 40, 260);
        caption = new EditBox(font, (width - boxWidth) / 2, height - 62, boxWidth, 18, Component.translatable("enriquecraft.share.caption"));
        caption.setMaxLength(200);
        caption.setHint(Component.translatable("enriquecraft.share.caption"));
        addRenderableWidget(caption);
        setInitialFocus(caption);

        int total = 3 * BUTTON_WIDTH + 2 * GAP;
        int left = (width - total) / 2;
        int top = height - 36;
        addRenderableWidget(Button.builder(Component.translatable("enriquecraft.share.send"), button -> {
            ScreenshotSender.send(file, caption.getValue());
            onClose();
        }).bounds(left, top, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("enriquecraft.share.copy"), button -> ScreenshotClipboard.copy(file))
                .bounds(left + BUTTON_WIDTH + GAP, top, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("enriquecraft.share.cancel"), button -> onClose())
                .bounds(left + 2 * (BUTTON_WIDTH + GAP), top, BUTTON_WIDTH, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title.getString(), width / 2, 12, 0xFFFFFFFF);
        if (texture != null) {
            int sourceWidth = texture.getPixels().getWidth();
            int sourceHeight = texture.getPixels().getHeight();
            int room = height - 24 - 80;
            double scale = Math.min(Math.min(1.0, (width - 40) / (double) sourceWidth), room / (double) sourceHeight);
            int drawWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
            int drawHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
            int x = (width - drawWidth) / 2;
            int y = 24 + (room - drawHeight) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, PREVIEW, x, y, 0.0F, 0.0F, drawWidth, drawHeight, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        }
        graphics.centeredText(font, Component.translatable("enriquecraft.share.warning").getString(), width / 2, height - 80 + ChatImageLayout.LINE_HEIGHT, WARNING_COLOR);
    }

    @Override
    public void removed() {
        if (texture != null) {
            minecraft.getTextureManager().release(PREVIEW);
            texture = null;
        }
        super.removed();
    }
}
