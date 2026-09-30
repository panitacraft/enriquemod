package com.panita.enriquecraft.core.gui;

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

    /** What a slot shows, or null when it is empty. */
    public static MenuItem itemAt(Menu menu, int slot) {
        return menu.itemAt(slot);
    }
}
