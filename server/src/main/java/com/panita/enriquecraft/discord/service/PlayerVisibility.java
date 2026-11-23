package com.panita.enriquecraft.discord.service;

import net.minecraft.server.level.ServerPlayer;

/**
 * Whether a player is hidden from the others, as the Vanish mod hides them. A hidden player is not reported to Discord as being in
 * the game, however they got there.
 */
@FunctionalInterface
public interface PlayerVisibility {

    /** For a server without a mod that hides players: nobody is hidden. */
    PlayerVisibility NOBODY_HIDDEN = player -> false;

    boolean isHidden(ServerPlayer player);
}
