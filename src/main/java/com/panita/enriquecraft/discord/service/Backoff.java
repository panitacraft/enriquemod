package com.panita.enriquecraft.discord.service;

import java.time.Duration;

/**
 * How long to wait before the next try to connect: one second, doubling up to a minute, with some randomness so many
 * servers that lose the bot together do not all come back at the same moment.
 */
public final class Backoff {

    private static final long BASE_MILLIS = 1_000;
    private static final long MAX_MILLIS = 60_000;

    private Backoff() {
    }

    /**
     * @param attempt how many times it has failed in a row, from zero
     * @param random  a number from 0 up to, not including, 1
     */
    public static Duration delay(int attempt, double random) {
        long base = Math.min(MAX_MILLIS, BASE_MILLIS << Math.min(attempt, 20));
        long jittered = Math.round(base * (0.8 + 0.4 * random));
        return Duration.ofMillis(Math.min(MAX_MILLIS, jittered));
    }
}
