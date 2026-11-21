package com.panita.enriquecraft.discord.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackoffTest {

    @Test
    void waitsOneSecondThenDoublesWithTheMiddleOfTheRandomness() {
        assertEquals(Duration.ofMillis(1000), Backoff.delay(0, 0.5));
        assertEquals(Duration.ofMillis(2000), Backoff.delay(1, 0.5));
        assertEquals(Duration.ofMillis(4000), Backoff.delay(2, 0.5));
        assertEquals(Duration.ofMillis(32000), Backoff.delay(5, 0.5));
    }

    @Test
    void neverWaitsMoreThanAMinute() {
        assertEquals(Duration.ofSeconds(60), Backoff.delay(6, 0.5));
        assertEquals(Duration.ofSeconds(60), Backoff.delay(30, 0.99));
        assertEquals(Duration.ofSeconds(60), Backoff.delay(Integer.MAX_VALUE, 0.99));
    }

    @Test
    void spreadsTheWaitByTwentyPercentEitherWay() {
        assertEquals(Duration.ofMillis(800), Backoff.delay(0, 0.0));
        assertTrue(Backoff.delay(0, 0.999).toMillis() <= 1200);
        assertTrue(Backoff.delay(3, 0.0).toMillis() >= 6400);
    }
}
