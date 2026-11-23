package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.network.MediaOpenS2C;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a click on the label of an image does for a player whose client companion shows images: the client opens its own viewer, so
 * nothing is downloaded or put in their hand by the server. The vanilla fallback, the map, is {@link ImageMaps}.
 */
public final class MediaViewer {

    private final ClientCapabilities capabilities;
    private final DiscordConfig config;

    public MediaViewer(ClientCapabilities capabilities, DiscordConfig config) {
        this.capabilities = capabilities;
        this.config = config;
    }

    /**
     * @return whether the client was asked to open the image; false when the player has no such client or the image is not on an
     *         allowed host, so the caller falls back to the map
     */
    public boolean open(ServerPlayer player, MediaEntry entry) {
        if (!capabilities.supports(player.getUUID(), ClientFeature.MEDIA_PREVIEW) || !ServerPlayNetworking.canSend(player, MediaOpenS2C.TYPE)
                || !MediaLinks.allows(entry, MediaLinks.hostsOf(config.mediaHosts.get()))) {
            return false;
        }
        ServerPlayNetworking.send(player, new MediaOpenS2C(entry.url()));
        return true;
    }
}
