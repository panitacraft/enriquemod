package com.panita.enriquecraft.core.gui;

import net.minecraft.world.inventory.ContainerInput;

/**
 * Lets tests in other packages draw a menu and look at its slots without opening it for a player.
 * Lives in this package to reach what is package-private, so production code stays as narrow as
 * it is.
 */
public final class MenuTesting {

    private MenuTesting() {
    }

    /** Draws the menu as opening it would, without a player. */
    public static void draw(Menu menu) {
        menu.prepare();
    }

    /** Delivers an ordinary left click on a slot, as the chest screen does, without a player. */
    public static void click(Menu menu, int slot) {
        menu.handleClick(new MenuClick(null, slot, 0, ContainerInput.PICKUP));
    }

    /** What a slot shows, or null when it is empty. */
    public static MenuItem itemAt(Menu menu, int slot) {
        return menu.itemAt(slot);
    }
}
