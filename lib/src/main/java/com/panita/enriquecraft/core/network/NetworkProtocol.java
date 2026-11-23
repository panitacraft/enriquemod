package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.discord.network.MediaOpenS2C;
import com.panita.enriquecraft.discord.network.MediaPreviewS2C;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareEndC2S;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What the client companion and the server must agree on before exchanging custom payloads.
 */
public final class NetworkProtocol {

    /** The namespace of every channel, font and texture of the mod, shared by the server mod and the client companion. */
    public static final String NAMESPACE = "enriquecraft";

    /**
     * Bumped on every change to a payload's layout. The server treats a client with a different
     * version as vanilla, so a mismatch degrades to the vanilla experience instead of breaking.
     */
    public static final int VERSION = 18;

    private NetworkProtocol() {
    }

    /** The type of a payload; its channel id is {@code enriquecraft:<path>}. */
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NAMESPACE, path));
    }

    /**
     * Registers every payload type. Both the server mod and the client companion call it once at startup: each side needs the
     * types to send and to read them, and each jar is installed on its own side only.
     */
    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(OpenUiS2C.TYPE, OpenUiS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdateUiS2C.TYPE, UpdateUiS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CloseUiS2C.TYPE, CloseUiS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InlineArtS2C.TYPE, InlineArtS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MediaPreviewS2C.TYPE, MediaPreviewS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MediaOpenS2C.TYPE, MediaOpenS2C.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HelloC2S.TYPE, HelloC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiClickC2S.TYPE, UiClickC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiSubmitC2S.TYPE, UiSubmitC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiSelectC2S.TYPE, UiSelectC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiDropC2S.TYPE, UiDropC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiClosedC2S.TYPE, UiClosedC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShareBeginC2S.TYPE, ShareBeginC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShareChunkC2S.TYPE, ShareChunkC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShareEndC2S.TYPE, ShareEndC2S.STREAM_CODEC);
    }
}
