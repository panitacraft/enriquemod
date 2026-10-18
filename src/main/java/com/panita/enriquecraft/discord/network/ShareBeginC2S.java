package com.panita.enriquecraft.discord.network;

import com.panita.enriquecraft.core.network.NetworkProtocol;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A client says it is about to send a screenshot, in pieces. Everything in it is untrusted: the server checks the size, the
 * destination and the caption before it accepts a single byte.
 *
 * @param id          chosen by the client to tell the pieces of this transfer; only matched against the transfer in progress
 * @param size        the bytes that will follow, in all
 * @param destination the {@link ShareDestination} as its ordinal
 * @param caption     what the player wrote, as it was typed
 */
public record ShareBeginC2S(int id, int size, int destination, String caption) implements CustomPacketPayload {

    public static final int MAX_CAPTION = 400;

    public static final Type<ShareBeginC2S> TYPE = NetworkProtocol.type("share_begin");

    public static final StreamCodec<ByteBuf, ShareBeginC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShareBeginC2S::id,
            ByteBufCodecs.VAR_INT, ShareBeginC2S::size,
            ByteBufCodecs.VAR_INT, ShareBeginC2S::destination,
            ByteBufCodecs.stringUtf8(MAX_CAPTION), ShareBeginC2S::caption,
            ShareBeginC2S::new);

    @Override
    public Type<ShareBeginC2S> type() {
        return TYPE;
    }
}
