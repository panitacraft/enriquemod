package com.panita.enriquecraft.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Blocks an address that keeps typing wrong PINs: every wrong PIN is counted the moment it is typed, and an
 * address with too many of them within a window is refused for a while. Counting each one, rather than the
 * kicks they lead to, is what stops a player who disconnects just before being kicked, or reconnects for
 * three fresh attempts each time, from guessing without limit. The counts live in memory only, so a restart
 * clears them.
 */
public final class AttemptLimiter {

    private static final class Record {
        private final Deque<Instant> failures = new ArrayDeque<>();
        private volatile Instant blockedUntil = Instant.MIN;
    }

    private final Map<String, Record> records = new ConcurrentHashMap<>();
    private final Supplier<Instant> clock;
    private final IntSupplier failuresBeforeBlock;
    private final Supplier<Duration> window;
    private final Supplier<Duration> blockFor;

    /**
     * @param failuresBeforeBlock how many wrong PINs within the window block an address
     * @param window              how long a wrong PIN keeps counting
     * @param blockFor            how long a block lasts
     */
    public AttemptLimiter(Supplier<Instant> clock, IntSupplier failuresBeforeBlock, Supplier<Duration> window,
                          Supplier<Duration> blockFor) {
        this.clock = clock;
        this.failuresBeforeBlock = failuresBeforeBlock;
        this.window = window;
        this.blockFor = blockFor;
    }

    /**
     * Counts a wrong PIN typed from an address.
     *
     * @return whether the address is blocked now
     */
    public boolean recordFailure(String address) {
        Instant now = clock.get();
        Duration counting = window.get();
        boolean[] blocked = new boolean[1];
        records.compute(address, (key, record) -> {
            Record current = record == null ? new Record() : record;
            current.failures.removeIf(failure -> failure.plus(counting).isBefore(now));
            current.failures.addLast(now);
            if (current.failures.size() >= failuresBeforeBlock.getAsInt()) {
                current.blockedUntil = now.plus(blockFor.get());
                current.failures.clear();
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
