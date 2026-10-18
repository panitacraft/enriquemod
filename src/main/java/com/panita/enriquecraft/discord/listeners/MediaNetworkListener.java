package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.network.MediaPreviewS2C;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareEndC2S;
import com.panita.enriquecraft.discord.service.ScreenshotShares;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Registers the payloads of the images in the chat of a client companion (for both sides, as the client needs the types too)
 * and hands the pieces of a screenshot a client sends to {@link ScreenshotShares}, which checks everything.
 */
public final class MediaNetworkListener implements ModListener {

    private final ScreenshotShares shares;

    public MediaNetworkListener(ScreenshotShares shares) {
        this.shares = shares;
    }

    @Override
    public void register() {
        PayloadTypeRegistry.clientboundPlay().register(MediaPreviewS2C.TYPE, MediaPreviewS2C.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShareBeginC2S.TYPE, ShareBeginC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShareChunkC2S.TYPE, ShareChunkC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShareEndC2S.TYPE, ShareEndC2S.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShareBeginC2S.TYPE, (begin, context) -> shares.begin(context.player(), begin));
        ServerPlayNetworking.registerGlobalReceiver(ShareChunkC2S.TYPE, (chunk, context) -> shares.chunk(context.player(), chunk));
        ServerPlayNetworking.registerGlobalReceiver(ShareEndC2S.TYPE, (end, context) -> shares.end(context.player(), end));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> shares.forget(handler.getPlayer().getUUID()));
    }
}
