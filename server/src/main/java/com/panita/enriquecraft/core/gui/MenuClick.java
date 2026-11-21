package com.panita.enriquecraft.core.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;

/**
 * A click on a slot of a menu.
 *
 * @param player the player who clicked
 * @param slot   the clicked menu slot
 * @param button the mouse button (0 left, 1 right) or, for other click types, the raw button value
 * @param input  the kind of click
 */
public record MenuClick(ServerPlayer player, int slot, int button, ContainerInput input) {

    /**
     * Whether this is an ordinary mouse click, with or without shift. Number-key swaps, drops,
     * drags, clones and double-click gathers are not plain.
     */
    public boolean isPlain() {
        return input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE;
    }

    public boolean isShift() {
        return input == ContainerInput.QUICK_MOVE;
    }

    public boolean isLeft() {
        return isPlain() && button == 0;
    }

    public boolean isRight() {
        return isPlain() && button == 1;
    }
}
