package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Forgets what a player was sent when they leave, so a client companion that rejoins is told about each image again.
 */
public final class InlineArtListener implements ModListener {

    private final InlineArts arts;

    public InlineArtListener(InlineArts arts) {
        this.arts = arts;
    }

    @Override
    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> arts.forget(handler.getPlayer().getUUID()));
    }
}
