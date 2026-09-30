package com.panita.enriquecraft.core.message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats moments for text shown to players, in the server's time zone.
 */
public final class Timestamps {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Timestamps() {
    }

    public static String format(Instant instant) {
        return format(instant, ZoneId.systemDefault());
    }

    public static String format(Instant instant, ZoneId zone) {
        return FORMAT.withZone(zone).format(instant);
    }
}
