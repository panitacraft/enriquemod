package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A thin vertical line between two columns of a row. It has no height of its own: the row that holds it stretches it to
 * the height of its tallest element. Inert, like the other decorations.
 */
final class UiVerticalRuleWidget extends AbstractWidget {

    private static final int WIDTH = 1;

    UiVerticalRuleWidget() {
        super(0, 0, WIDTH, 0, Component.empty());
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + WIDTH, getY() + getHeight(), UiTheme.DIVIDER);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
