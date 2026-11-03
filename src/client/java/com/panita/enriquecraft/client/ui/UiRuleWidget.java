package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A short thin line, centered, that groups what is above it apart from what is below. Inert, like the other decorations. */
final class UiRuleWidget extends AbstractWidget {

    private static final int WIDTH = 96;
    private static final int HEIGHT = 1;

    UiRuleWidget() {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
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
