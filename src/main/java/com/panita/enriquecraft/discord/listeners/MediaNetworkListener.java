package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.network.MediaPreviewS2C;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers the payload that tells a client companion about the image of a chat line (for both sides, as the client needs the type too). */
public final class MediaNetworkListener implements ModListener {

    @Override
    public void register() {
        PayloadTypeRegistry.clientboundPlay().register(MediaPreviewS2C.TYPE, MediaPreviewS2C.STREAM_CODEC);
    }
}
