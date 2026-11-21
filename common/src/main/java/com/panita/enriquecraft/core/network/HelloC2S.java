package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Collection;
import java.util.List;

/**
 * Sent by the client companion when it joins a server that understands it: which protocol it speaks
 * and which enhancements it implements. Everything in it is untrusted; see {@link ClientCapabilities}.
 *
 * @param featureIds {@link ClientFeature#id()} values; unknown ids are ignored by the server
 */
public record HelloC2S(int protocolVersion, List<String> featureIds) implements CustomPacketPayload {

    // Bounds keep a forged payload from making the server allocate arbitrarily much.
    private static final int MAX_FEATURES = 32;
    private static final int MAX_FEATURE_ID_LENGTH = 64;

    public static final Type<HelloC2S> TYPE = NetworkProtocol.type("hello");

    public static final StreamCodec<ByteBuf, HelloC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HelloC2S::protocolVersion,
            ByteBufCodecs.stringUtf8(MAX_FEATURE_ID_LENGTH).apply(ByteBufCodecs.list(MAX_FEATURES)), HelloC2S::featureIds,
            HelloC2S::new);

    /** The hello of a client that speaks the current protocol and implements the given features. */
    public static HelloC2S announcing(Collection<ClientFeature> features) {
        return new HelloC2S(NetworkProtocol.VERSION, features.stream().map(ClientFeature::id).toList());
    }

    @Override
    public Type<HelloC2S> type() {
        return TYPE;
    }
}
