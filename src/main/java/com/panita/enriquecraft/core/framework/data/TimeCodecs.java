package com.panita.enriquecraft.core.framework.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Codecs for time values, written as readable text in the data files.
 */
public final class TimeCodecs {

    /** An instant written as ISO-8601 text, for example {@code 2026-09-30T04:12:00Z}. */
    public static final Codec<Instant> INSTANT = Codec.STRING.comapFlatMap(TimeCodecs::parse, Instant::toString);

    private TimeCodecs() {
    }

    private static DataResult<Instant> parse(String text) {
        try {
            return DataResult.success(Instant.parse(text));
        } catch (DateTimeParseException e) {
            return DataResult.error(() -> "Not a valid date and time: " + text);
        }
    }
}
