package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareEndC2S;
import com.panita.enriquecraft.discord.service.ScreenshotShares;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Hands the pieces of a screenshot a client sends to {@link ScreenshotShares}, which checks everything.
 */
public final class MediaNetworkListener implements ModListener {

    private final ScreenshotShares shares;

    public MediaNetworkListener(ScreenshotShares shares) {
        this.shares = shares;
    }

    @Override
    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(ShareBeginC2S.TYPE, (begin, context) -> shares.begin(context.player(), begin));
        ServerPlayNetworking.registerGlobalReceiver(ShareChunkC2S.TYPE, (chunk, context) -> shares.chunk(context.player(), chunk));
        ServerPlayNetworking.registerGlobalReceiver(ShareEndC2S.TYPE, (end, context) -> shares.end(context.player(), end));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> shares.forget(handler.getPlayer().getUUID()));
    }
}
