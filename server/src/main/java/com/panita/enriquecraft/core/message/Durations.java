package com.panita.enriquecraft.core.message;

import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats spans of time for text shown to players: the largest unit and, when it is not zero, the next one
 * ({@code 1d}, {@code 1d2h}, {@code 18h13m}, {@code 10m}, {@code 5s}), and reads the same notation back, extended with weeks
 * ({@code 1w1d1m1s}).
 */
public final class Durations {

    private static final long MINUTE = 60;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    /** A span longer than this is a typing mistake, and would overflow the dates the game stores. */
    private static final Duration LONGEST = Duration.ofDays(36_500);
    private static final Pattern WHOLE = Pattern.compile("(?:\\d{1,9}[wdhms])+");
    private static final Pattern PART = Pattern.compile("(\\d+)([wdhms])");

    private Durations() {
    }

    /**
     * Reads a span written as amounts followed by their unit, largest first or in any order but each unit once:
     * {@code w} weeks, {@code d} days, {@code h} hours, {@code m} minutes and {@code s} seconds.
     *
     * @return the span, or empty when the text is not written that way, is zero, or is longer than 100 years
     */
    public static Optional<Duration> parse(String text) {
        String lowered = text.toLowerCase(Locale.ROOT);
        if (!WHOLE.matcher(lowered).matches()) {
            return Optional.empty();
        }
        Set<Character> seen = new HashSet<>();
        long seconds = 0;
        Matcher part = PART.matcher(lowered);
        while (part.find()) {
            char unit = part.group(2).charAt(0);
            if (!seen.add(unit)) {
                return Optional.empty();
            }
            seconds += Long.parseLong(part.group(1)) * secondsPer(unit);
        }
        Duration duration = Duration.ofSeconds(seconds);
        return duration.isZero() || duration.compareTo(LONGEST) > 0 ? Optional.empty() : Optional.of(duration);
    }

    private static long secondsPer(char unit) {
        return switch (unit) {
            case 'w' -> WEEK;
            case 'd' -> DAY;
            case 'h' -> HOUR;
            case 'm' -> MINUTE;
            default -> 1;
        };
    }

    public static String format(Duration duration) {
        // Round up: a span that still has a fraction of a second left is not over yet.
        long seconds = Math.max(0, duration.plusNanos(999_999_999).toSeconds());
        if (seconds >= DAY) {
            return unit(seconds / DAY, "d") + unit(seconds % DAY / HOUR, "h");
        }
        if (seconds >= HOUR) {
            return unit(seconds / HOUR, "h") + unit(seconds % HOUR / MINUTE, "m");
        }
        if (seconds >= MINUTE) {
            return unit(seconds / MINUTE, "m") + unit(seconds % MINUTE, "s");
        }
        return seconds + "s";
    }

    private static String unit(long amount, String suffix) {
        return amount == 0 ? "" : amount + suffix;
    }
}
