package com.panita.enriquecraft.discord.service;

import java.time.Duration;

/**
 * How long to wait before the next try to connect: one second, doubling up to a minute, with some randomness so many
 * servers that lose the bot together do not all come back at the same moment. A line that another server took over waits much
 * longer, since coming back at once only takes the line back from it, and it then does the same.
 */
public final class Backoff {

    private static final long BASE_MILLIS = 1_000;
    private static final long MAX_MILLIS = 60_000;
    private static final long REPLACED_BASE_MILLIS = 30_000;
    private static final long REPLACED_MAX_MILLIS = 600_000;

    private Backoff() {
    }

    /**
     * @param attempt how many times it has failed in a row, from zero
     * @param random  a number from 0 up to, not including, 1
     */
    public static Duration delay(int attempt, double random) {
        return spread(BASE_MILLIS, MAX_MILLIS, attempt, random);
    }

    /**
     * The wait after another server took the line: thirty seconds, doubling up to ten minutes.
     *
     * @param times how many times in a row the line was taken, from zero
     * @param random a number from 0 up to, not including, 1
     */
    public static Duration afterReplaced(int times, double random) {
        return spread(REPLACED_BASE_MILLIS, REPLACED_MAX_MILLIS, times, random);
    }

    private static Duration spread(long baseMillis, long maxMillis, int count, double random) {
        long base = Math.min(maxMillis, baseMillis << Math.min(count, 20));
        long jittered = Math.round(base * (0.8 + 0.4 * random));
        return Duration.ofMillis(Math.min(maxMillis, jittered));
    }
}
