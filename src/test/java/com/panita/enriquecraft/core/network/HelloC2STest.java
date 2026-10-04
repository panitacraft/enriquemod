package com.panita.enriquecraft.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HelloC2STest {

    @Test
    void survivesAnEncodeDecodeRoundTrip() {
        HelloC2S hello = HelloC2S.announcing(EnumSet.of(ClientFeature.CUSTOM_UI));
        ByteBuf buffer = Unpooled.buffer();

        HelloC2S.STREAM_CODEC.encode(buffer, hello);

        assertEquals(hello, HelloC2S.STREAM_CODEC.decode(buffer));
    }

    @Test
    void announcesTheCurrentProtocolVersion() {
        assertEquals(NetworkProtocol.VERSION, HelloC2S.announcing(EnumSet.noneOf(ClientFeature.class)).protocolVersion());
    }

    @Test
    void refusesToEncodeMoreFeaturesThanTheServerWillRead() {
        HelloC2S tooMany = new HelloC2S(NetworkProtocol.VERSION, Collections.nCopies(33, "x"));

        assertThrows(RuntimeException.class, () -> HelloC2S.STREAM_CODEC.encode(Unpooled.buffer(), tooMany));
    }

    @Test
    void refusesToEncodeAnOverlongFeatureId() {
        HelloC2S overlong = new HelloC2S(NetworkProtocol.VERSION, List.of("x".repeat(65)));

        assertThrows(RuntimeException.class, () -> HelloC2S.STREAM_CODEC.encode(Unpooled.buffer(), overlong));
    }
}
