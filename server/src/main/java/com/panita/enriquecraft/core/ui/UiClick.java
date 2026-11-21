package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.MenuClick;
import net.minecraft.server.level.ServerPlayer;

/**
 * A press on a button of a {@link UiMenu}, the same whether it came from a chest slot or from the
 * client companion's screen.
 *
 * @param player the player who pressed
 * @param button the mouse button: 0 is left, 1 is right
 * @param shift  whether shift was held; required for anything destructive
 */
public record UiClick(ServerPlayer player, int button, boolean shift) {

    /** The press behind an ordinary click on a chest slot. */
    static UiClick from(MenuClick click) {
        return new UiClick(click.player(), click.button(), click.isShift());
    }

    public boolean isLeft() {
        return button == 0;
    }

    public boolean isRight() {
        return button == 1;
    }

    public boolean isShift() {
        return shift;
    }
}
