package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The option the player chose in a dropdown of an open screen. Untrusted until {@code UiSessions}
 * has checked the dropdown exists and the position is one of its options.
 */
public record UiSelectC2S(int sessionId, int elementId, int option) implements CustomPacketPayload {

    public static final Type<UiSelectC2S> TYPE = NetworkProtocol.type("ui_select");

    public static final StreamCodec<ByteBuf, UiSelectC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UiSelectC2S::sessionId,
            ByteBufCodecs.VAR_INT, UiSelectC2S::elementId,
            ByteBufCodecs.VAR_INT, UiSelectC2S::option,
            UiSelectC2S::new);

    @Override
    public Type<UiSelectC2S> type() {
        return TYPE;
    }
}
