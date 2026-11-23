package com.panita.enriquecraft.client.media;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Shows one image of the chat as large as the window allows, which the player asked for by clicking its label. A click anywhere or
 * Escape closes it. The image is downloaded when the screen opens and its texture is released when it closes.
 */
final class MediaViewerScreen extends Screen {

    private static final int MARGIN = 20;
    private static final int HINT_ROOM = 24;
    /** The window cannot show more pixels than it has, and an image is not stretched past twice its own. */
    private static final int MAX_UPSCALE = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final String url;
    private MediaImage image;
    private boolean failed;
    private boolean closed;

    MediaViewerScreen(String url) {
        super(Component.translatable("enriquecraft.media.title"));
        this.url = url;
    }

    @Override
    protected void init() {
        if (image == null && !failed) {
            MediaImages.loadFull(url, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), this::loaded);
        }
    }

    private void loaded(Optional<MediaImage> result) {
        if (closed) {
            result.ifPresent(MediaImage::close);
        } else if (result.isPresent()) {
            image = result.get();
        } else {
            failed = true;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (image != null) {
            image.advance(System.currentTimeMillis());
            double guiScale = minecraft.getWindow().getGuiScale();
            double room = Math.min((width - 2.0 * MARGIN) / image.width(), (height - MARGIN - HINT_ROOM) / (double) image.height());
            double scale = Math.min(room, MAX_UPSCALE / guiScale);
            int drawWidth = Math.max(1, (int) Math.round(image.width() * scale));
            int drawHeight = Math.max(1, (int) Math.round(image.height() * scale));
            int x = (width - drawWidth) / 2;
            int y = MARGIN / 2 + (height - MARGIN - HINT_ROOM - drawHeight) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, image.id(), x, y, 0.0F, 0.0F, drawWidth, drawHeight,
                    image.width(), image.height(), image.width(), image.height());
        } else {
            String status = Component.translatable(failed ? "enriquecraft.media.failed" : "enriquecraft.media.loading").getString();
            graphics.centeredText(font, status, width / 2, height / 2 - font.lineHeight / 2, TEXT_COLOR);
        }
        graphics.centeredText(font, Component.translatable("enriquecraft.media.close").getString(), width / 2, height - HINT_ROOM / 2 - font.lineHeight / 2, HINT_COLOR);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        onClose();
        return true;
    }

    @Override
    public void removed() {
        closed = true;
        if (image != null) {
            image.close();
            image = null;
        }
        super.removed();
    }
}
