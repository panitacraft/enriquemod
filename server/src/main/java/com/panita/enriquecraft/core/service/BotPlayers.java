package com.panita.enriquecraft.core.service;

import net.minecraft.server.level.ServerPlayer;

/**
 * Tells a bot from a person. A bot is a player entity that no client is behind, such as the ones Carpet's {@code /player} spawns: it
 * never went through the login, so the mods that prepare a player there (LuckPerms loads their user, the PIN screen holds them) know
 * nothing of it. The game only ever creates a plain {@link ServerPlayer} for a connection, and every kind of bot is a subclass of it.
 */
public final class BotPlayers {

    private BotPlayers() {
    }

    public static boolean is(ServerPlayer player) {
        return player.getClass() != ServerPlayer.class;
    }
}
