package com.panita.enriquecraft.core.message;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampsTest {

    private static final Instant MOMENT = Instant.parse("2026-09-30T04:12:59Z");

    @Test
    void theDateIsDayMonthYearInTheGivenZone() {
        assertEquals("30/09/2026", Timestamps.date(MOMENT, ZoneId.of("UTC")));
        assertEquals("29/09/2026", Timestamps.date(MOMENT, ZoneId.of("America/El_Salvador")));
    }

    @Test
    void theDateTimeAddsTheTimeInParenthesesWithoutSeconds() {
        assertEquals("30/09/2026 (04:12)", Timestamps.dateTime(MOMENT, ZoneId.of("UTC")));
        assertEquals("29/09/2026 (22:12)", Timestamps.dateTime(MOMENT, ZoneId.of("America/El_Salvador")));
    }

    @Test
    void crossesMidnightCorrectly() {
        assertEquals("01/10/2026 (00:00)",
                Timestamps.dateTime(Instant.parse("2026-09-30T23:59:59Z").plusSeconds(1), ZoneId.of("UTC")));
    }

    @Test
    void singleDigitDaysAndHoursKeepTheirZero() {
        assertEquals("05/01/2026 (07:03)", Timestamps.dateTime(Instant.parse("2026-01-05T07:03:00Z"), ZoneId.of("UTC")));
    }
}
