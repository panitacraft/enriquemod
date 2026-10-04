package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * One line of text in a detail view. When the server marked it with a copy-to-clipboard click event, such as
 * an id, a click copies that value and the line says so. Only that one event is honored: the client never
 * runs anything else a server could attach to text.
 */
final class UiLineWidget extends AbstractWidget {

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final long FEEDBACK_MILLIS = 1500;

    private static final Component COPY_HINT = Component.translatable("enriquecraft.ui.copy");
    private static final Component COPIED = Component.translatable("enriquecraft.ui.copied");

    private final Font font;
    private final Component line;
    private final String copyValue;
    private long copiedAt = Long.MIN_VALUE / 2;

    UiLineWidget(Font font, Component line) {
        super(0, 0, font.width(line), font.lineHeight, line);
        this.font = font;
        this.line = line;
        this.copyValue = copyValueOf(line).orElse(null);
        // A line with nothing to copy is inert, like plain text.
        this.active = copyValue != null;
    }

    private static Optional<String> copyValueOf(Component line) {
        return line.visit((style, text) -> style.getClickEvent() instanceof ClickEvent.CopyToClipboard copy
                ? Optional.of(copy.value()) : Optional.empty(), Style.EMPTY);
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return button.button() == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        Minecraft.getInstance().keyboardHandler.setClipboard(copyValue);
        copiedAt = System.currentTimeMillis();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Clicking copies silently; the tooltip confirms it.
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isHovered();
        Component shown = hovered ? line.copy().withStyle(style -> style.withUnderlined(true)) : line;
        graphics.text(font, shown, getX(), getY(), TEXT_COLOR);
        if (hovered) {
            boolean justCopied = System.currentTimeMillis() - copiedAt < FEEDBACK_MILLIS;
            graphics.setTooltipForNextFrame(font, justCopied ? COPIED : COPY_HINT, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
