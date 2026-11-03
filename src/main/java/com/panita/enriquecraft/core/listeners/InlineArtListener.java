package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.network.InlineArtS2C;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Registers the payload that tells a client companion what an image of a line is, for both sides as the client needs the type too,
 * and forgets what a player was sent when they leave.
 */
public final class InlineArtListener implements ModListener {

    private final InlineArts arts;

    public InlineArtListener(InlineArts arts) {
        this.arts = arts;
    }

    @Override
    public void register() {
        PayloadTypeRegistry.clientboundPlay().register(InlineArtS2C.TYPE, InlineArtS2C.STREAM_CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> arts.forget(handler.getPlayer().getUUID()));
    }
}
