package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * An empty cell of a grid. It only keeps the grid readable as a grid, so it is inert: it makes no
 * sound and takes no clicks.
 */
final class UiCellWidget extends AbstractWidget {

    UiCellWidget() {
        super(0, 0, UiButtonWidget.CELL_SIZE, UiButtonWidget.CELL_SIZE, Component.empty());
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(), UiTheme.CELL_EMPTY, UiTheme.CELL_EMPTY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
