package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.chat.service.SkinMemory;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Keeps the skin of every player when they are let in and when they leave, so their head can be drawn while they are away. Players
 * still on the PIN screen are not kept: they are not in yet.
 */
public final class SkinMemoryListener implements ModListener {

    private final SkinMemory skins;

    public SkinMemoryListener(SkinMemory skins) {
        this.skins = skins;
    }

    @Override
    public void register() {
        AuthEvents.PLAYER_READY.register(player -> skins.remember(player.getGameProfile()));
        // The profile at the end of the visit has the skin the player changed to while playing
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> skins.remember(handler.getPlayer().getGameProfile()));
    }
}
