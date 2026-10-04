package com.panita.enriquecraft.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The title of a screen, in bold and larger than body text. Inert, like the cells of a grid.
 */
final class UiTitleWidget extends AbstractWidget {

    private static final float SCALE = 1.5F;

    private final Font font;
    private final Component text;

    UiTitleWidget(Font font, Component title) {
        super(0, 0, Math.round(font.width(title.copy().withStyle(ChatFormatting.BOLD)) * SCALE),
                Math.round(font.lineHeight * SCALE), title);
        this.font = font;
        this.text = title.copy().withStyle(ChatFormatting.BOLD);
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(SCALE, SCALE);
        graphics.text(font, text, 0, 0, UiTheme.TEXT);
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
