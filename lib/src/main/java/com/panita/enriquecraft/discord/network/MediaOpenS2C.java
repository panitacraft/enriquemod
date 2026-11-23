package com.panita.enriquecraft.discord.network;

import com.panita.enriquecraft.core.network.NetworkProtocol;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks a client that shows images to open its viewer on one: the answer to a click on the label of an image, for a player whose
 * client announced {@code ClientFeature.MEDIA_PREVIEW}. Other players get the image on a map instead.
 *
 * @param url the address of an image the server put in the chat, which it already checked against its allowed hosts
 */
public record MediaOpenS2C(String url) implements CustomPacketPayload {

    public static final Type<MediaOpenS2C> TYPE = NetworkProtocol.type("media_open");

    public static final StreamCodec<ByteBuf, MediaOpenS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MediaPreviewS2C.MAX_URL), MediaOpenS2C::url,
            MediaOpenS2C::new);

    @Override
    public Type<MediaOpenS2C> type() {
        return TYPE;
    }
}
