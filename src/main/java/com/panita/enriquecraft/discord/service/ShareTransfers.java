package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareDestination;
import com.panita.enriquecraft.discord.network.ShareEndC2S;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The screenshots clients are sending, one per player, assembled from their pieces. A client is untrusted, so every limit is
 * checked before bytes are kept: the size announced, the order of the pieces, the total, and how long a transfer may last. Used
 * on the server thread only. What comes out is still only bytes that someone sent, and has to be decoded and re-encoded.
 */
public final class ShareTransfers {

    /** The most a screenshot may weigh as it travels; the server re-encodes it to fit the bot's limit afterwards. */
    public static final int MAX_BYTES = 3 * 1024 * 1024;
    public static final int MAX_CAPTION = 200;
    public static final Duration TIMEOUT = Duration.ofSeconds(30);

    /** A transfer that arrived whole. */
    public record Completed(byte[] bytes, ShareDestination destination, String caption) {
    }

    public enum Rejection {
        BUSY,
        INVALID,
        TOO_LARGE
    }

    private static final class Transfer {
        private final int id;
        private final byte[] bytes;
        private final ShareDestination destination;
        private final String caption;
        private final Instant started;
        private int received;
        private int nextIndex;

        private Transfer(int id, int size, ShareDestination destination, String caption, Instant started) {
            this.id = id;
            this.bytes = new byte[size];
            this.destination = destination;
            this.caption = caption;
            this.started = started;
        }
    }

    private final Supplier<Instant> clock;
    private final Map<UUID, Transfer> transfers = new HashMap<>();

    public ShareTransfers(Supplier<Instant> clock) {
        this.clock = clock;
    }

    /** Starts a transfer; empty when it was accepted. A player already sending one waits for it, unless it has timed out. */
    public Optional<Rejection> begin(UUID player, ShareBeginC2S begin) {
        Transfer current = transfers.get(player);
        if (current != null && !expired(current)) {
            return Optional.of(Rejection.BUSY);
        }
        transfers.remove(player);
        Optional<ShareDestination> destination = ShareDestination.byOrdinal(begin.destination());
        if (begin.size() <= 0 || destination.isEmpty()) {
            return Optional.of(Rejection.INVALID);
        }
        if (begin.size() > MAX_BYTES) {
            return Optional.of(Rejection.TOO_LARGE);
        }
        transfers.put(player, new Transfer(begin.id(), begin.size(), destination.get(), PlainText.caption(begin.caption(), MAX_CAPTION), clock.get()));
        return Optional.empty();
    }

    /** Adds a piece; a piece that is not the next one of this transfer, or that overflows it, ends the transfer. */
    public void chunk(UUID player, ShareChunkC2S chunk) {
        Transfer transfer = transfers.get(player);
        if (transfer == null) {
            return;
        }
        int length = chunk.data().length;
        if (transfer.id != chunk.id() || expired(transfer) || chunk.index() != transfer.nextIndex || length == 0
                || length > ShareChunkC2S.MAX_BYTES || transfer.received + length > transfer.bytes.length) {
            transfers.remove(player);
            return;
        }
        System.arraycopy(chunk.data(), 0, transfer.bytes, transfer.received, length);
        transfer.received += length;
        transfer.nextIndex++;
    }

    /** Closes the transfer; the screenshot is given only when exactly the announced bytes arrived. */
    public Optional<Completed> end(UUID player, ShareEndC2S end) {
        Transfer transfer = transfers.get(player);
        if (transfer == null || transfer.id != end.id()) {
            return Optional.empty();
        }
        transfers.remove(player);
        if (expired(transfer) || transfer.received != transfer.bytes.length) {
            return Optional.empty();
        }
        return Optional.of(new Completed(transfer.bytes, transfer.destination, transfer.caption));
    }

    public void forget(UUID player) {
        transfers.remove(player);
    }

    private boolean expired(Transfer transfer) {
        return Duration.between(transfer.started, clock.get()).compareTo(TIMEOUT) > 0;
    }
}
