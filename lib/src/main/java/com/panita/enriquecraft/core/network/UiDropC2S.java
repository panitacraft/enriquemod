package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A draggable button of an open screen dropped onto another one. Only identifies the two buttons: what
 * moving one onto the other means is decided by the server, and everything here is untrusted until
 * {@code UiSessions} has checked it.
 */
public record UiDropC2S(int sessionId, int draggedId, int targetId) implements CustomPacketPayload {

    public static final Type<UiDropC2S> TYPE = NetworkProtocol.type("ui_drop");

    public static final StreamCodec<ByteBuf, UiDropC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UiDropC2S::sessionId,
            ByteBufCodecs.VAR_INT, UiDropC2S::draggedId,
            ByteBufCodecs.VAR_INT, UiDropC2S::targetId,
            UiDropC2S::new);

    @Override
    public Type<UiDropC2S> type() {
        return TYPE;
    }
}
