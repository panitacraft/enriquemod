package com.panita.enriquecraft.auth.service;

import net.minecraft.server.level.ServerPlayer;

/**
 * Shows the PIN screen to a player, in whatever form their client allows. Implemented by the screens
 * themselves, so the services never depend on them.
 */
public interface PinScreens {

    /** A PIN screen that is on a player's display. */
    interface Screen {
        /** Shows the flow's current state. */
        void redraw();

        /** Takes the screen away. */
        void close();
    }

    /**
     * Opens the screen.
     *
     * @param onDismissed runs when the screen goes away by any means, including the player closing it
     */
    Screen open(ServerPlayer player, PinFlow flow, Runnable onDismissed);
}
