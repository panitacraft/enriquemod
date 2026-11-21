package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The value the player confirmed in a text field of an open screen. Untrusted until
 * {@code UiSessions} has checked it against the field.
 */
public record UiSubmitC2S(int sessionId, int elementId, String text) implements CustomPacketPayload {

    public static final Type<UiSubmitC2S> TYPE = NetworkProtocol.type("ui_submit");

    public static final StreamCodec<ByteBuf, UiSubmitC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UiSubmitC2S::sessionId,
            ByteBufCodecs.VAR_INT, UiSubmitC2S::elementId,
            // Only what a text area can hold fits the wire; UiSessions holds a single-line field to its own, smaller limit.
            ByteBufCodecs.stringUtf8(UiElement.TextArea.MAX_LENGTH), UiSubmitC2S::text,
            UiSubmitC2S::new);

    @Override
    public Type<UiSubmitC2S> type() {
        return TYPE;
    }
}
