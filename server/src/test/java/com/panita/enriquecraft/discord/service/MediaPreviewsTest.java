package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.network.MediaPreviewS2C;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaPreviewsTest {

    private static MediaEntry entry(String url, boolean spoiler) {
        return new MediaEntry(url, "a.png", "image/png", 10, OptionalInt.empty(), OptionalInt.empty(), spoiler);
    }

    @Test
    void aSpoilerIsNotPreviewed() {
        MediaEntry plain = entry("https://cdn.discordapp.com/a.png", false);
        assertEquals(List.of(plain), MediaPreviews.previewable(List.of(plain, entry("https://cdn.discordapp.com/b.png", true))));
    }

    @Test
    void thePayloadSurvivesItsCodec() {
        ByteBuf buffer = Unpooled.buffer();
        MediaPreviewS2C.STREAM_CODEC.encode(buffer, new MediaPreviewS2C("https://cdn.discordapp.com/a.png", 640, 480));
        assertEquals(new MediaPreviewS2C("https://cdn.discordapp.com/a.png", 640, 480), MediaPreviewS2C.STREAM_CODEC.decode(buffer));
    }

    @Test
    void anAddressPastTheLimitIsNotEncoded() {
        String url = "https://cdn.discordapp.com/" + "a".repeat(MediaPreviewS2C.MAX_URL);
        assertThrows(RuntimeException.class, () -> MediaPreviewS2C.STREAM_CODEC.encode(Unpooled.buffer(), new MediaPreviewS2C(url, 1, 1)));
    }

    @Test
    void theClientOnlyTrustsAFixedListOfHosts() {
        assertTrue(MediaLinks.isTrusted("https://cdn.discordapp.com/a.png"));
        assertTrue(MediaLinks.isTrusted("https://media.tenor.com/x.gif"));
        assertFalse(MediaLinks.isTrusted("http://cdn.discordapp.com/a.png"));
        assertFalse(MediaLinks.isTrusted("https://localhost/a.png"));
        assertFalse(MediaLinks.isTrusted("https://tenor.com/x.gif"));
        assertFalse(MediaLinks.isTrusted("https://evilcdn.discordapp.com.example.com/a.png"));
        assertFalse(MediaLinks.isTrusted("not an address"));
    }

    @Test
    void aPreviewFitsItsRoomWithoutGrowingOrLosingItsShape() {
        assertEquals(new PreviewLayout.Size(100, 50), PreviewLayout.fit(100, 50));
        assertEquals(new PreviewLayout.Size(200, 100), PreviewLayout.fit(1000, 500));
        assertEquals(new PreviewLayout.Size(15, 150), PreviewLayout.fit(100, 1000));
        assertThrows(IllegalArgumentException.class, () -> PreviewLayout.fit(0, 5));
    }
}
