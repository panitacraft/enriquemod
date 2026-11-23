package com.panita.enriquecraft.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpsMonitorTest {

    private static final long MILLI = 1_000_000L;
    private static final long SECOND = 1_000 * MILLI;

    private long now = SECOND;
    private final TpsMonitor monitor = new TpsMonitor(() -> now);

    /** Runs the server for the given seconds with this long between ticks. */
    private void run(int seconds, long tickNanos, boolean noPlayers) {
        long end = now + seconds * SECOND;
        while (now < end) {
            now += tickNanos;
            monitor.tick(noPlayers);
        }
    }

    @Test
    void beforeAnyTickTheServerIsAssumedToRunAtTheTarget() {
        assertEquals(new TpsMonitor.Averages(20, 20, 20), monitor.averages());
    }

    @Test
    void aServerOnTimeReadsTwentyForEveryWindow() {
        run(120, 50 * MILLI, false);

        TpsMonitor.Averages averages = monitor.averages();
        assertEquals(20.0, averages.oneMinute(), 0.05);
        assertEquals(20.0, averages.fiveMinutes(), 0.05);
        assertEquals(20.0, averages.fifteenMinutes(), 0.05);
    }

    @Test
    void aSlowServerReadsTheRateItReallyRuns() {
        run(120, 100 * MILLI, false);

        assertEquals(10.0, monitor.averages().oneMinute(), 0.1);
    }

    @Test
    void theLongerWindowsRememberWhatTheShorterOnesForget() {
        run(240, 50 * MILLI, false);
        run(60, 100 * MILLI, false);

        TpsMonitor.Averages averages = monitor.averages();
        assertEquals(10.0, averages.oneMinute(), 0.3);
        assertEquals(18.0, averages.fiveMinutes(), 0.3);
        assertEquals(18.0, averages.fifteenMinutes(), 0.3);
    }

    @Test
    void theTimeAnEmptyServerSpentPausedIsNotCountedAsLag() {
        run(60, 50 * MILLI, true);
        now += 600 * SECOND;
        run(60, 50 * MILLI, false);

        assertEquals(20.0, monitor.averages().fifteenMinutes(), 0.1);
    }

    @Test
    void aLagSpikeWithPlayersOnlineLowersTheAverage() {
        run(30, 50 * MILLI, false);
        now += 10 * SECOND;
        monitor.tick(false);
        run(30, 50 * MILLI, false);

        assertTrue(monitor.averages().oneMinute() < 19.0);
    }
}
