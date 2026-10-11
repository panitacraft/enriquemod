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
    private int failuresBeforeBlock = 3;
    private final AttemptLimiter limiter = new AttemptLimiter(() -> now, () -> failuresBeforeBlock, () -> WINDOW, () -> BLOCK);

    @Test
    void anAddressIsBlockedAfterEnoughWrongPinsWithinTheWindow() {
        assertFalse(limiter.recordFailure("a"));
        now = now.plusSeconds(60);
        assertFalse(limiter.recordFailure("a"));
        now = now.plusSeconds(60);

        assertTrue(limiter.recordFailure("a"));
        assertEquals(BLOCK, limiter.blockedFor("a").orElseThrow());
    }

    @Test
    void wrongPinsOutsideTheWindowDoNotCount() {
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        now = now.plus(WINDOW).plusSeconds(1);

        assertFalse(limiter.recordFailure("a"));
        assertTrue(limiter.blockedFor("a").isEmpty());
    }

    @Test
    void theBlockEndsAndOtherAddressesAreNotAffected() {
        for (int failure = 0; failure < 3; failure++) {
            limiter.recordFailure("a");
        }

        assertTrue(limiter.blockedFor("b").isEmpty());
        now = now.plus(BLOCK).plusSeconds(1);
        assertTrue(limiter.blockedFor("a").isEmpty());
    }

    @Test
    void clearingForgetsTheWrongPins() {
        failuresBeforeBlock = 2;
        limiter.recordFailure("a");
        limiter.clear("a");

        assertFalse(limiter.recordFailure("a"));
    }

    @Test
    void theLimitIsReadWhenItIsNeededSoAReloadTakesEffect() {
        failuresBeforeBlock = 5;
        for (int failure = 0; failure < 3; failure++) {
            limiter.recordFailure("a");
        }
        failuresBeforeBlock = 4;

        assertTrue(limiter.recordFailure("a"));
    }
}
