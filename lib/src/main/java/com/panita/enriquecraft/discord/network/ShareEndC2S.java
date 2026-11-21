package com.panita.enriquecraft.discord.network;

import com.panita.enriquecraft.core.network.NetworkProtocol;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The client has sent every piece of the transfer in progress. */
public record ShareEndC2S(int id) implements CustomPacketPayload {

    public static final Type<ShareEndC2S> TYPE = NetworkProtocol.type("share_end");

    public static final StreamCodec<ByteBuf, ShareEndC2S> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ShareEndC2S::new, ShareEndC2S::id);

    @Override
    public Type<ShareEndC2S> type() {
        return TYPE;
    }
}
