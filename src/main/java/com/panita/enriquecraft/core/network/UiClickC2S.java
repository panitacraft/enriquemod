package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A press on a button of an open screen. Only identifies the button: what it does is decided by the
 * server, and everything here is untrusted until {@code UiSessions} has checked it.
 *
 * @param button the mouse button: 0 is left, 1 is right
 */
public record UiClickC2S(int sessionId, int elementId, int button, boolean shift) implements CustomPacketPayload {

    public static final Type<UiClickC2S> TYPE = NetworkProtocol.type("ui_click");

    public static final StreamCodec<ByteBuf, UiClickC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UiClickC2S::sessionId,
            ByteBufCodecs.VAR_INT, UiClickC2S::elementId,
            ByteBufCodecs.VAR_INT, UiClickC2S::button,
            ByteBufCodecs.BOOL, UiClickC2S::shift,
            UiClickC2S::new);

    @Override
    public Type<UiClickC2S> type() {
        return TYPE;
    }
}
