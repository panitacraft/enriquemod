package com.panita.enriquecraft.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Blocks an address that keeps failing the PIN screen: each kick for too many wrong PINs is counted,
 * and an address kicked too many times within a window is refused for a while. The counts live in
 * memory only, so a restart clears them.
 */
public final class AttemptLimiter {

    private static final class Record {
        private final Deque<Instant> kicks = new ArrayDeque<>();
        private volatile Instant blockedUntil = Instant.MIN;
    }

    private final Map<String, Record> records = new ConcurrentHashMap<>();
    private final Supplier<Instant> clock;

    public AttemptLimiter(Supplier<Instant> clock) {
        this.clock = clock;
    }

    /**
     * Counts a kick for an address.
     *
     * @param kicksBeforeBlock how many kicks within the window block the address
     * @param window           how long a kick keeps counting
     * @param blockFor         how long the block lasts
     * @return whether the address is blocked now
     */
    public boolean recordKick(String address, int kicksBeforeBlock, Duration window, Duration blockFor) {
        Instant now = clock.get();
        boolean[] blocked = new boolean[1];
        records.compute(address, (key, record) -> {
            Record current = record == null ? new Record() : record;
            current.kicks.removeIf(kick -> kick.plus(window).isBefore(now));
            current.kicks.addLast(now);
            if (current.kicks.size() >= kicksBeforeBlock) {
                current.blockedUntil = now.plus(blockFor);
                current.kicks.clear();
                blocked[0] = true;
            }
            return current;
        });
        return blocked[0];
    }

    /** How much longer the address is blocked, or empty when it is not. */
    public Optional<Duration> blockedFor(String address) {
        Record record = records.get(address);
        if (record == null) {
            return Optional.empty();
        }
        Instant now = clock.get();
        Instant until = record.blockedUntil;
        return until.isAfter(now) ? Optional.of(Duration.between(now, until)) : Optional.empty();
    }

    /** Forgets an address, for example after a correct PIN. */
    public void clear(String address) {
        records.remove(address);
    }
}
