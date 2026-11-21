package com.panita.enriquecraft.core.message;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

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

    @Test
    void parsesEveryUnitInAnyCase() {
        assertEquals(Optional.of(Duration.ofSeconds(7 * 86_400 + 86_400 + 60 + 1)), Durations.parse("1w1d1m1s"));
        assertEquals(Optional.of(Duration.ofHours(36)), Durations.parse("36H"));
        assertEquals(Optional.of(Duration.ofMinutes(90)), Durations.parse("1h30m"));
        assertEquals(Optional.of(Duration.ofMinutes(90)), Durations.parse("30m1h"));
    }

    @Test
    void refusesWhatIsNotASpan() {
        for (String text : new String[]{"", "d", "10", "1d1d", "1 d", "1x", "-1d", "1.5h", "griefing", "1dgriefing", "0s", "0d0h"}) {
            assertEquals(Optional.empty(), Durations.parse(text), text);
        }
    }

    @Test
    void refusesASpanTooLongToStore() {
        assertEquals(Optional.empty(), Durations.parse("999999999w"));
        assertEquals(Optional.empty(), Durations.parse("5215w"));
        assertEquals(Optional.of(Duration.ofDays(5214 * 7)), Durations.parse("5214w"));
    }
}
