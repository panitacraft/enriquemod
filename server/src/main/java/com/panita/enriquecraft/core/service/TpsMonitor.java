package com.panita.enriquecraft.core.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * Measures how many ticks the server really runs each second, averaged over the last 1, 5 and 15 minutes as Spigot's {@code /tps}
 * does. The game keeps no such figure, only the time of its last hundred ticks.
 * <p>
 * Ticks are counted in buckets of about a second. An average adds up the newest buckets until they cover its window, so time the
 * server did not run is never counted: a server that pauses itself when empty stops ticking, and the long gap before the first
 * tick after it is dropped instead of reading as a crawl. Called only from the server thread.
 */
public final class TpsMonitor {

    /** The rate the game aims for; an average is never shown above it. */
    public static final double TARGET = 20.0;

    private static final long BUCKET_NANOS = 1_000_000_000L;
    /** A gap this long between two ticks, after a tick with nobody online, is the server having been paused, not a lag spike. */
    private static final long PAUSE_GAP_NANOS = 2_000_000_000L;
    private static final long MINUTE_NANOS = 60 * BUCKET_NANOS;
    private static final int KEPT_BUCKETS = 15 * 60 + 2;

    /**
     * The average ticks per second of the last one, five and fifteen minutes, or of as much time as has been measured since the
     * server started.
     */
    public record Averages(double oneMinute, double fiveMinutes, double fifteenMinutes) {
    }

    private record Bucket(int ticks, long nanos) {
    }

    private final LongSupplier clock;
    private final Deque<Bucket> history = new ArrayDeque<>();
    private long bucketStart;
    private int bucketTicks;
    private long lastTick;
    private boolean lastTickHadNoPlayers;

    /** @param clock the time in nanoseconds, such as {@code System::nanoTime} */
    public TpsMonitor(LongSupplier clock) {
        this.clock = clock;
    }

    /** Counts one tick of the server, which has just finished. */
    public void tick(boolean noPlayersOnline) {
        long now = clock.getAsLong();
        boolean resumed = lastTick != 0 && lastTickHadNoPlayers && now - lastTick > PAUSE_GAP_NANOS;
        lastTick = now;
        lastTickHadNoPlayers = noPlayersOnline;
        if (bucketStart == 0 || resumed) {
            // The first tick only says when to start counting, and so does the first one after a pause
            bucketStart = now;
            bucketTicks = 0;
            return;
        }
        bucketTicks++;
        if (now - bucketStart >= BUCKET_NANOS) {
            history.addLast(new Bucket(bucketTicks, now - bucketStart));
            if (history.size() > KEPT_BUCKETS) {
                history.removeFirst();
            }
            bucketStart = now;
            bucketTicks = 0;
        }
    }

    public Averages averages() {
        return new Averages(over(MINUTE_NANOS), over(5 * MINUTE_NANOS), over(15 * MINUTE_NANOS));
    }

    private double over(long windowNanos) {
        long covered = 0;
        long ticks = 0;
        for (var buckets = history.descendingIterator(); buckets.hasNext() && covered < windowNanos; ) {
            Bucket bucket = buckets.next();
            covered += bucket.nanos();
            ticks += bucket.ticks();
        }
        return covered == 0 ? TARGET : ticks * (double) BUCKET_NANOS / covered;
    }
}
