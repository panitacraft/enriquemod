package com.panita.enriquecraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * The whole player who is viewing the screen, as the inventory shows them: with their own skin and turning to follow the mouse. It is
 * the client's own player, so the skin is the one the server gave them. Inert, like the other decorations.
 */
final class UiPlayerModelWidget extends AbstractWidget {

    /** The size the game gives the model for each pixel of height of its box, as the inventory does. */
    private static final float SIZE_PER_HEIGHT = 0.4F;
    private static final float OFFSET_Y = 0.0625F;

    UiPlayerModelWidget(int width, int height) {
        super(0, 0, width, height, Component.empty());
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                Math.round(getHeight() * SIZE_PER_HEIGHT), OFFSET_Y, mouseX, mouseY, Minecraft.getInstance().player);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
