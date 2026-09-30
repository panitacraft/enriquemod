package com.panita.enriquecraft.core.message;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampsTest {

    private static final Instant MOMENT = Instant.parse("2026-09-30T04:12:59Z");

    @Test
    void formatsInTheGivenZoneWithoutSeconds() {
        assertEquals("2026-09-30 04:12", Timestamps.format(MOMENT, ZoneId.of("UTC")));
        assertEquals("2026-09-29 22:12", Timestamps.format(MOMENT, ZoneId.of("America/El_Salvador")));
    }

    @Test
    void crossesMidnightCorrectly() {
        assertEquals("2026-10-01 00:00", Timestamps.format(Instant.parse("2026-09-30T23:59:59Z").plusSeconds(1), ZoneId.of("UTC")));
    }
}
