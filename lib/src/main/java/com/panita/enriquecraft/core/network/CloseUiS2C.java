package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks the client companion to close a screen the server opened.
 */
public record CloseUiS2C(int sessionId) implements CustomPacketPayload {

    public static final Type<CloseUiS2C> TYPE = NetworkProtocol.type("close_ui");

    public static final StreamCodec<ByteBuf, CloseUiS2C> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CloseUiS2C::new, CloseUiS2C::sessionId);

    @Override
    public Type<CloseUiS2C> type() {
        return TYPE;
    }
}
