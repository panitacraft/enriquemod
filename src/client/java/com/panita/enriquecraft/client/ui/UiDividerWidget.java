package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A thin line that separates two parts of a screen. It has no width of its own: the column that holds
 * it stretches it to the width of the widest element beside it. Inert, like the other decorations.
 */
final class UiDividerWidget extends AbstractWidget {

    private static final int HEIGHT = 1;

    UiDividerWidget() {
        super(0, 0, 0, HEIGHT, Component.empty());
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + HEIGHT, UiTheme.DIVIDER);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
