package com.panita.enriquecraft.message.channel;

import java.time.Duration;

/**
 * Timings of a title: how long it fades in, stays fully visible, and fades out.
 */
public record TitleTimes(Duration fadeIn, Duration stay, Duration fadeOut) {

    /** Vanilla defaults: 10, 70 and 20 ticks. */
    public static final TitleTimes DEFAULT =
            new TitleTimes(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000));

    private static final long MILLIS_PER_TICK = 50;

    int fadeInTicks() {
        return toTicks(fadeIn);
    }

    int stayTicks() {
        return toTicks(stay);
    }

    int fadeOutTicks() {
        return toTicks(fadeOut);
    }

    private static int toTicks(Duration duration) {
        return (int) (duration.toMillis() / MILLIS_PER_TICK);
    }
}
