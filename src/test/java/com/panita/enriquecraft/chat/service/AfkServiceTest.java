package com.panita.enriquecraft.chat.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfkServiceTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AfkService afk = new AfkService(now::get, enabled::get, () -> 60, () -> "&7");
    private final UUID player = UUID.randomUUID();

    private void pass(int seconds) {
        now.addAndGet(seconds * 1000L);
    }

    @Test
    void aPlayerNeverSeenIsNotIdle() {
        assertFalse(afk.isIdle(player));
    }

    @Test
    void aPlayerIsIdleOnlyAfterTheTimeout() {
        afk.touch(player);

        pass(59);
        assertFalse(afk.isIdle(player));
        pass(1);
        assertTrue(afk.isIdle(player));
    }

    @Test
    void anySignOfActivityEndsIt() {
        afk.touch(player);
        pass(120);
        assertTrue(afk.isIdle(player));

        afk.touch(player);

        assertFalse(afk.isIdle(player));
    }

    @Test
    void turningTheViewIsActivityButStandingStillIsNot() {
        afk.observe(player, 10f, 5f);
        pass(50);
        afk.observe(player, 10f, 5f);
        pass(10);
        assertTrue(afk.isIdle(player));

        afk.observe(player, 40f, 5f);

        assertFalse(afk.isIdle(player));
    }

    @Test
    void theFirstLookOnlyRecordsTheView() {
        afk.touch(player);
        pass(60);

        afk.observe(player, 90f, 0f);

        assertTrue(afk.isIdle(player));
    }

    @Test
    void nobodyIsIdleWhileItIsOff() {
        afk.touch(player);
        pass(600);
        enabled.set(false);

        assertFalse(afk.isIdle(player));
    }

    @Test
    void aChangeIsReportedOnceInEachDirection() {
        assertEquals(Optional.empty(), afk.transition(player, false));
        assertEquals(Optional.of(true), afk.transition(player, true));
        assertEquals(Optional.empty(), afk.transition(player, true));
        assertEquals(Optional.of(false), afk.transition(player, false));
    }

    @Test
    void aPlayerWhoLeftStartsOverNotAfk() {
        afk.transition(player, true);

        afk.forget(player);

        assertEquals(Optional.empty(), afk.transition(player, false));
    }

    @Test
    void aPlayerWhoLeftIsForgotten() {
        afk.touch(player);
        pass(600);

        afk.forget(player);

        assertFalse(afk.isIdle(player));
    }
}
