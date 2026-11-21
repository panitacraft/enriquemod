package com.panita.enriquecraft.chat.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameChangesTest {

    private final NameChanges changes = new NameChanges();
    private final UUID player = UUID.randomUUID();

    @Test
    void theFirstNameIsAChange() {
        assertTrue(changes.changed(player, "a"));
    }

    @Test
    void theSameNameIsNotSentAgain() {
        changes.changed(player, "a");

        assertFalse(changes.changed(player, "a"));
    }

    @Test
    void aDifferentNameIsAChangeEachTime() {
        changes.changed(player, "a");

        assertTrue(changes.changed(player, "b"));
        assertTrue(changes.changed(player, "a"));
    }

    @Test
    void aPlayerWhoLeftStartsOver() {
        changes.changed(player, "a");

        changes.forget(player);

        assertTrue(changes.changed(player, "a"));
    }

    @Test
    void playersAreKeptApart() {
        changes.changed(player, "a");

        assertTrue(changes.changed(UUID.randomUUID(), "a"));
    }
}
