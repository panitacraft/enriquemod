package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareEndC2S;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareTransfersTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private Instant now = Instant.parse("2026-10-18T12:00:00Z");
    private final ShareTransfers transfers = new ShareTransfers(() -> now);

    private static ShareBeginC2S begin(int size) {
        return new ShareBeginC2S(7, size, "  mi base\u0007​ ");
    }

    @Test
    void piecesInOrderMakeTheScreenshotWithACleanCaption() {
        assertEquals(Optional.empty(), transfers.begin(PLAYER, begin(5)));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 0, new byte[]{1, 2, 3}));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 1, new byte[]{4, 5}));
        ShareTransfers.Completed completed = transfers.end(PLAYER, new ShareEndC2S(7)).orElseThrow();
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, completed.bytes());
        assertEquals("mi base", completed.caption());
    }

    @Test
    void aTransferThatIsNotWholeGivesNothing() {
        transfers.begin(PLAYER, begin(5));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 0, new byte[]{1, 2, 3}));
        assertTrue(transfers.end(PLAYER, new ShareEndC2S(7)).isEmpty());
    }

    @Test
    void aPieceOutOfOrderOrTooBigEndsTheTransfer() {
        transfers.begin(PLAYER, begin(5));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 1, new byte[]{1}));
        assertTrue(transfers.end(PLAYER, new ShareEndC2S(7)).isEmpty());

        transfers.begin(PLAYER, begin(2));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 0, new byte[]{1, 2, 3}));
        assertTrue(transfers.end(PLAYER, new ShareEndC2S(7)).isEmpty());
    }

    @Test
    void anotherIdOrAnEmptyPieceDoesNotCount() {
        transfers.begin(PLAYER, begin(1));
        transfers.chunk(PLAYER, new ShareChunkC2S(8, 0, new byte[]{1}));
        assertTrue(transfers.end(PLAYER, new ShareEndC2S(8)).isEmpty());

        transfers.begin(PLAYER, begin(1));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 0, new byte[0]));
        assertTrue(transfers.end(PLAYER, new ShareEndC2S(7)).isEmpty());
    }

    @Test
    void sizesAreChecked() {
        assertEquals(Optional.of(ShareTransfers.Rejection.INVALID), transfers.begin(PLAYER, begin(0)));
        assertEquals(Optional.of(ShareTransfers.Rejection.TOO_LARGE), transfers.begin(PLAYER, begin(ShareTransfers.MAX_BYTES + 1)));
        assertEquals(Optional.empty(), transfers.begin(PLAYER, begin(ShareTransfers.MAX_BYTES)));
    }

    @Test
    void oneTransferAtATimeUntilItTimesOut() {
        assertEquals(Optional.empty(), transfers.begin(PLAYER, begin(5)));
        assertEquals(Optional.of(ShareTransfers.Rejection.BUSY), transfers.begin(PLAYER, begin(5)));
        now = now.plus(ShareTransfers.TIMEOUT).plusSeconds(1);
        assertEquals(Optional.empty(), transfers.begin(PLAYER, begin(5)));
    }

    @Test
    void aTransferThatTookTooLongGivesNothing() {
        transfers.begin(PLAYER, begin(1));
        transfers.chunk(PLAYER, new ShareChunkC2S(7, 0, new byte[]{1}));
        now = now.plus(Duration.ofSeconds(31));
        assertTrue(transfers.end(PLAYER, new ShareEndC2S(7)).isEmpty());
    }

    @Test
    void thePayloadsSurviveTheirCodecs() {
        ByteBuf buffer = Unpooled.buffer();
        ShareChunkC2S chunk = new ShareChunkC2S(3, 2, new byte[]{9, 8, 7});
        ShareChunkC2S.STREAM_CODEC.encode(buffer, chunk);
        ShareChunkC2S decoded = ShareChunkC2S.STREAM_CODEC.decode(buffer);
        assertEquals(3, decoded.id());
        assertEquals(2, decoded.index());
        assertArrayEquals(new byte[]{9, 8, 7}, decoded.data());

        ShareBeginC2S.STREAM_CODEC.encode(buffer, begin(5));
        assertEquals(begin(5), ShareBeginC2S.STREAM_CODEC.decode(buffer));
        ShareEndC2S.STREAM_CODEC.encode(buffer, new ShareEndC2S(4));
        assertEquals(new ShareEndC2S(4), ShareEndC2S.STREAM_CODEC.decode(buffer));
    }
}
