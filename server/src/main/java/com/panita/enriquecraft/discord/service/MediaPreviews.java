package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.discord.network.MediaPreviewS2C;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Tells the players whose client shows images in the chat which images the line they just read carries. A player without that
 * client reads the same line and gets nothing extra, which is the vanilla fallback: the label, the map and the link.
 */
public final class MediaPreviews {

    private final ClientCapabilities capabilities;

    public MediaPreviews(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    /** The images worth showing in the chat: not a spoiler, whose picture a reader has not asked to see. */
    static List<MediaEntry> previewable(List<MediaEntry> images) {
        return images.stream().filter(image -> !image.spoiler()).toList();
    }

    /** Sent right after the line was delivered, to the players who read it and announced the feature. */
    public void send(Collection<ServerPlayer> readers, List<MediaEntry> images) {
        List<MediaEntry> previews = previewable(images);
        if (previews.isEmpty()) {
            return;
        }
        for (ServerPlayer reader : readers) {
            if (capabilities.supports(reader.getUUID(), ClientFeature.MEDIA_PREVIEW) && ServerPlayNetworking.canSend(reader, MediaPreviewS2C.TYPE)) {
                for (MediaEntry image : previews) {
                    ServerPlayNetworking.send(reader, new MediaPreviewS2C(image.url(), image.width().orElse(0), image.height().orElse(0)));
                }
            }
        }
    }
}
