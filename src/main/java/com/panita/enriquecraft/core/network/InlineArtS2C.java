package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.core.art.ArtLinks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a client that draws art inside the chat what one of the glyphs it is about to receive is: the image to fetch. Sent once for
 * each image, before the first line that holds it, and only to players who announced {@code ClientFeature.INLINE_ART}.
 *
 * @param id  the short name of the address, as the font of the glyph is named
 * @param url the address the image is fetched from, which the server already checked against the trusted hosts
 */
public record InlineArtS2C(String id, String url) implements CustomPacketPayload {

    public static final Type<InlineArtS2C> TYPE = NetworkProtocol.type("inline_art");

    public static final StreamCodec<ByteBuf, InlineArtS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16), InlineArtS2C::id,
            ByteBufCodecs.stringUtf8(ArtLinks.MAX_URL), InlineArtS2C::url,
            InlineArtS2C::new);

    @Override
    public Type<InlineArtS2C> type() {
        return TYPE;
    }
}
