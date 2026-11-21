package com.panita.enriquecraft.auth.listeners;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Connects the players who still have to enter their PIN to the game: sends them to the waiting spot as they
 * join, keeps their screen up, and lets nothing hurt them meanwhile.
 */
public final class HeldPlayersListener implements ModListener {

    private final HeldPlayers held;

    public HeldPlayersListener(HeldPlayers held) {
        this.held = held;
    }

    @Override
    public void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> held.begin(handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(server -> held.tick());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> !(entity instanceof ServerPlayer player && held.isHeld(player)));
    }
}
