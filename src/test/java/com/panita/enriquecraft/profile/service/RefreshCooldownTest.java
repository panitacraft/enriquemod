package com.panita.enriquecraft.profile.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefreshCooldownTest {

    private final UUID player = UUID.randomUUID();
    private Instant now = Instant.parse("2026-11-03T12:00:00Z");
    private int seconds = 60;
    private final RefreshCooldown cooldown = new RefreshCooldown(() -> seconds, () -> now);

    @Test
    void aPlayerWhoNeverRefreshedMayRefreshAtOnce() {
        assertEquals(0, cooldown.remainingSeconds(player));
    }

    @Test
    void afterARefreshThePlayerWaitsTheSetTimeRoundedUp() {
        cooldown.start(player);
        now = now.plusMillis(30_500);

        assertEquals(30, cooldown.remainingSeconds(player));
        now = now.plusMillis(29_000);
        assertEquals(1, cooldown.remainingSeconds(player));
        now = now.plusMillis(600);
        assertEquals(0, cooldown.remainingSeconds(player));
    }

    @Test
    void aWaitOfZeroTurnsTheCooldownOff() {
        seconds = 0;
        cooldown.start(player);

        assertEquals(0, cooldown.remainingSeconds(player));
    }

    @Test
    void theSettingIsReadAtEachUse() {
        cooldown.start(player);
        seconds = 10;
        now = now.plusSeconds(11);

        assertEquals(0, cooldown.remainingSeconds(player));
    }

    @Test
    void aPlayerWhoLeavesStartsOver() {
        cooldown.start(player);
        cooldown.forget(player);

        assertEquals(0, cooldown.remainingSeconds(player));
    }

    @Test
    void playersWaitIndependently() {
        cooldown.start(player);

        assertEquals(0, cooldown.remainingSeconds(UUID.randomUUID()));
    }
}
