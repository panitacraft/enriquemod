package com.panita.enriquecraft.core.message;

import java.time.Duration;

/**
 * Formats spans of time for text shown to players: the largest unit and, when it is not zero, the next one
 * ({@code 1d}, {@code 1d2h}, {@code 18h13m}, {@code 10m}, {@code 5s}).
 */
public final class Durations {

    private static final long MINUTE = 60;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private Durations() {
    }

    public static String format(Duration duration) {
        // Round up: a span that still has a fraction of a second left is not over yet.
        long seconds = Math.max(0, duration.plusNanos(999_999_999).toSeconds());
        if (seconds >= DAY) {
            return unit(seconds / DAY, "d") + unit(seconds % DAY / HOUR, "h");
        }
        if (seconds >= HOUR) {
            return unit(seconds / HOUR, "h") + unit(seconds % HOUR / MINUTE, "m");
        }
        if (seconds >= MINUTE) {
            return unit(seconds / MINUTE, "m") + unit(seconds % MINUTE, "s");
        }
        return seconds + "s";
    }

    private static String unit(long amount, String suffix) {
        return amount == 0 ? "" : amount + suffix;
    }
}
