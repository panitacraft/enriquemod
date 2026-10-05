package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * An item drawn at any size, for the icon beside a title and for the large item of a detail view.
 * Inert, like the other decorations.
 */
final class UiItemWidget extends AbstractWidget {

    private static final float ITEM_SIZE = 16.0F;

    private final ItemStack stack;

    UiItemWidget(ItemStack stack, int size) {
        super(0, 0, size, size, Component.empty());
        this.stack = stack;
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(getWidth() / ITEM_SIZE, getHeight() / ITEM_SIZE);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
