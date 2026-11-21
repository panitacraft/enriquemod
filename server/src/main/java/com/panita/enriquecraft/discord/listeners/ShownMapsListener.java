package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.service.ShownMaps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Ends the maps shown to players that ran out or no longer apply, and forgets the ones of players who left. */
public final class ShownMapsListener implements ModListener {

    private final ShownMaps maps;

    public ShownMapsListener(ShownMaps maps) {
        this.maps = maps;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(maps::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> maps.forget(handler.getPlayer().getUUID()));
    }
}
