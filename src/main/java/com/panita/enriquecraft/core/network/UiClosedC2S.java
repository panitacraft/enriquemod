package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the server the player's screen is gone, however it went: the player closed it, or another
 * screen replaced it.
 */
public record UiClosedC2S(int sessionId) implements CustomPacketPayload {

    public static final Type<UiClosedC2S> TYPE = NetworkProtocol.type("ui_closed");

    public static final StreamCodec<ByteBuf, UiClosedC2S> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(UiClosedC2S::new, UiClosedC2S::sessionId);

    @Override
    public Type<UiClosedC2S> type() {
        return TYPE;
    }
}
