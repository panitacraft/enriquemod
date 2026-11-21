package com.panita.enriquecraft.core.framework.data;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeCodecsTest {

    @Test
    void instantIsWrittenAsIsoText() {
        Instant instant = Instant.parse("2026-09-30T04:12:00Z");

        var json = TimeCodecs.INSTANT.encodeStart(JsonOps.INSTANCE, instant).getOrThrow();

        assertEquals(new JsonPrimitive("2026-09-30T04:12:00Z"), json);
    }

    @Test
    void instantRoundTrips() {
        Instant instant = Instant.parse("2026-09-30T04:12:00.123Z");

        var json = TimeCodecs.INSTANT.encodeStart(JsonOps.INSTANCE, instant).getOrThrow();

        assertEquals(instant, TimeCodecs.INSTANT.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void textThatIsNotADateIsAnErrorNotAnException() {
        var result = TimeCodecs.INSTANT.parse(JsonOps.INSTANCE, new JsonPrimitive("yesterday"));

        assertTrue(result.error().isPresent());
    }

    @Test
    void aNumberIsNotADate() {
        assertTrue(TimeCodecs.INSTANT.parse(JsonOps.INSTANCE, new JsonPrimitive(5)).error().isPresent());
    }
}
