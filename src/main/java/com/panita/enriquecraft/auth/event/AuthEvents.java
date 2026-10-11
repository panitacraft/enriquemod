package com.panita.enriquecraft.auth.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * What happens to a player's login, for the modules that must wait for it, such as the one that welcomes players.
 */
public final class AuthEvents {

    private AuthEvents() {
    }

    /**
     * A player is in and may play: they joined with their Microsoft account or a remembered session, or entered the right
     * PIN. Called on the server thread, once for each join.
     */
    public static final Event<PlayerReady> PLAYER_READY = EventFactory.createArrayBacked(PlayerReady.class,
            listeners -> player -> {
                for (PlayerReady listener : listeners) {
                    listener.onReady(player);
                }
            });

    @FunctionalInterface
    public interface PlayerReady {

        void onReady(ServerPlayer player);
    }
}
