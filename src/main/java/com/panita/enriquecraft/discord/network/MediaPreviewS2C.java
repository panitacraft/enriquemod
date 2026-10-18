package com.panita.enriquecraft.discord.network;

import com.panita.enriquecraft.core.network.NetworkProtocol;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a client that shows images in the chat that the line it just received carries one, which it can fetch itself from the
 * address. Sent only to players who announced {@code ClientFeature.MEDIA_PREVIEW}, after the line, which is the same for every
 * player and links to the same address, so that is how the client knows which line it belongs to.
 *
 * @param url    the address the line links to, which the server already checked against its allowed hosts
 * @param width  the width Discord gave, or 0 when it did not say
 * @param height the height Discord gave, or 0 when it did not say
 */
public record MediaPreviewS2C(String url, int width, int height) implements CustomPacketPayload {

    public static final int MAX_URL = 1024;

    public static final Type<MediaPreviewS2C> TYPE = NetworkProtocol.type("media_preview");

    public static final StreamCodec<ByteBuf, MediaPreviewS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_URL), MediaPreviewS2C::url,
            ByteBufCodecs.VAR_INT, MediaPreviewS2C::width,
            ByteBufCodecs.VAR_INT, MediaPreviewS2C::height,
            MediaPreviewS2C::new);

    @Override
    public Type<MediaPreviewS2C> type() {
        return TYPE;
    }
}
