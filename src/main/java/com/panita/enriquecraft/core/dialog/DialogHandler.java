package com.panita.enriquecraft.core.dialog;

import net.minecraft.server.level.ServerPlayer;

/** What a dialog does when one of its buttons is pressed. Runs on the server thread, with validated values. */
@FunctionalInterface
public interface DialogHandler {

    /**
     * @param button the key of the pressed button
     * @param values what the player entered, already cleaned
     */
    void handle(ServerPlayer player, String button, DialogValues values);
}
