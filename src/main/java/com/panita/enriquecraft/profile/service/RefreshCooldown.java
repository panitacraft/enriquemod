package com.panita.enriquecraft.profile.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * How long a player must wait between two refreshes of their own profile, so the command and the button cannot be used to
 * flood the API. It lives in memory and is dropped when the player leaves.
 */
public final class RefreshCooldown {

    private final IntSupplier seconds;
    private final Supplier<Instant> clock;
    private final Map<UUID, Instant> last = new ConcurrentHashMap<>();

    public RefreshCooldown(IntSupplier seconds, Supplier<Instant> clock) {
        this.seconds = seconds;
        this.clock = clock;
    }

    /** Whole seconds the player still has to wait, rounded up; 0 when they may refresh now. */
    public long remainingSeconds(UUID player) {
        Instant started = last.get(player);
        if (started == null) {
            return 0;
        }
        Duration left = Duration.between(clock.get(), started.plusSeconds(seconds.getAsInt()));
        return left.isNegative() || left.isZero() ? 0 : (left.toMillis() + 999) / 1000;
    }

    /** Starts the wait. */
    public void start(UUID player) {
        last.put(player, clock.get());
    }

    public void forget(UUID player) {
        last.remove(player);
    }
}
