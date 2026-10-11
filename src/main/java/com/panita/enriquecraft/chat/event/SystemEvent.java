package com.panita.enriquecraft.chat.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Something the game tells everyone about a player: that they came, left, died or made an advancement.
 */
public sealed interface SystemEvent {

    ServerPlayer player();

    /**
     * @param first   whether it is the first time the player joins the server
     * @param oldName the name they had before, when they changed it since the last time
     */
    record Joined(ServerPlayer player, boolean first, Optional<String> oldName) implements SystemEvent {
    }

    record Left(ServerPlayer player) implements SystemEvent {
    }

    /** @param message the game's own text about the death, which the client translates */
    record Died(ServerPlayer player, Component message) implements SystemEvent {
    }

    /** @param message the game's own text about the advancement, which the client translates */
    record Advanced(ServerPlayer player, Component message) implements SystemEvent {
    }
}
