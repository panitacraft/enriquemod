package com.panita.enriquecraft.core.message;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationsTest {

    @Test
    void secondsAreShownAlone() {
        assertEquals("5s", Durations.format(Duration.ofSeconds(5)));
        assertEquals("0s", Durations.format(Duration.ZERO));
        assertEquals("0s", Durations.format(Duration.ofSeconds(-3)));
    }

    @Test
    void minutesAreFollowedByTheSecondsThatRemain() {
        assertEquals("10m", Durations.format(Duration.ofMinutes(10)));
        assertEquals("1m30s", Durations.format(Duration.ofSeconds(90)));
    }

    @Test
    void hoursAreFollowedByTheMinutesThatRemain() {
        assertEquals("18h13m", Durations.format(Duration.ofHours(18).plusMinutes(13).plusSeconds(40)));
        assertEquals("1h", Durations.format(Duration.ofHours(1).plusSeconds(30)));
    }

    @Test
    void daysAreFollowedByTheHoursThatRemain() {
        assertEquals("1d", Durations.format(Duration.ofDays(1)));
        assertEquals("2d6h", Durations.format(Duration.ofDays(2).plusHours(6).plusMinutes(5)));
    }

    @Test
    void aFractionOfASecondCountsAsTheWholeSecond() {
        assertEquals("1s", Durations.format(Duration.ofMillis(200)));
    }
}
