package com.panita.enriquecraft.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK = Duration.ofMinutes(15);

    private Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final AttemptLimiter limiter = new AttemptLimiter(() -> now);

    @Test
    void anAddressIsBlockedAfterEnoughKicksWithinTheWindow() {
        assertFalse(limiter.recordKick("a", 3, WINDOW, BLOCK));
        now = now.plusSeconds(60);
        assertFalse(limiter.recordKick("a", 3, WINDOW, BLOCK));
        now = now.plusSeconds(60);

        assertTrue(limiter.recordKick("a", 3, WINDOW, BLOCK));
        assertEquals(BLOCK, limiter.blockedFor("a").orElseThrow());
    }

    @Test
    void kicksOutsideTheWindowDoNotCount() {
        limiter.recordKick("a", 3, WINDOW, BLOCK);
        limiter.recordKick("a", 3, WINDOW, BLOCK);
        now = now.plus(WINDOW).plusSeconds(1);

        assertFalse(limiter.recordKick("a", 3, WINDOW, BLOCK));
        assertTrue(limiter.blockedFor("a").isEmpty());
    }

    @Test
    void theBlockEndsAndOtherAddressesAreNotAffected() {
        for (int kick = 0; kick < 3; kick++) {
            limiter.recordKick("a", 3, WINDOW, BLOCK);
        }

        assertTrue(limiter.blockedFor("b").isEmpty());
        now = now.plus(BLOCK).plusSeconds(1);
        assertTrue(limiter.blockedFor("a").isEmpty());
    }

    @Test
    void clearingForgetsTheKicks() {
        limiter.recordKick("a", 2, WINDOW, BLOCK);
        limiter.clear("a");

        assertFalse(limiter.recordKick("a", 2, WINDOW, BLOCK));
    }
}
