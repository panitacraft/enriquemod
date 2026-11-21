package com.panita.enriquecraft.discord.network;

import com.panita.enriquecraft.core.network.NetworkProtocol;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * One piece of the screenshot of the transfer in progress. A packet from a client may not pass 32,767 bytes, so a piece is
 * at most {@link #MAX_BYTES}.
 *
 * @param index the position of the piece, counted from 0 and without gaps
 */
public record ShareChunkC2S(int id, int index, byte[] data) implements CustomPacketPayload {

    public static final int MAX_BYTES = 30 * 1024;

    public static final Type<ShareChunkC2S> TYPE = NetworkProtocol.type("share_chunk");

    public static final StreamCodec<ByteBuf, ShareChunkC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShareChunkC2S::id,
            ByteBufCodecs.VAR_INT, ShareChunkC2S::index,
            ByteBufCodecs.byteArray(MAX_BYTES), ShareChunkC2S::data,
            ShareChunkC2S::new);

    @Override
    public Type<ShareChunkC2S> type() {
        return TYPE;
    }
}
