package com.panita.enriquecraft.core.message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats moments for text shown to players, in the server's time zone, day first as players read dates.
 */
public final class Timestamps {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy (HH:mm)");

    private Timestamps() {
    }

    /** The day, such as {@code 30/09/2026}. */
    public static String date(Instant instant) {
        return date(instant, ZoneId.systemDefault());
    }

    public static String date(Instant instant, ZoneId zone) {
        return DATE.withZone(zone).format(instant);
    }

    /** The day and the time, such as {@code 30/09/2026 (04:12)}. */
    public static String dateTime(Instant instant) {
        return dateTime(instant, ZoneId.systemDefault());
    }

    public static String dateTime(Instant instant, ZoneId zone) {
        return DATE_TIME.withZone(zone).format(instant);
    }
}
