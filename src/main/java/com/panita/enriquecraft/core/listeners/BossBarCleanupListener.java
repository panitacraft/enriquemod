package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.message.channel.BossBarChannel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Releases boss bars when their owner disconnects or the server stops.
 */
public final class BossBarCleanupListener {

    private final BossBarChannel bossBars;

    public BossBarCleanupListener(BossBarChannel bossBars) {
        this.bossBars = bossBars;
    }

    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> bossBars.removeAll(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> bossBars.clear());
    }
}
