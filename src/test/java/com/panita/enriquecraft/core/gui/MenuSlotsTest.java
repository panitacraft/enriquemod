package com.panita.enriquecraft.core.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuSlotsTest {

    private final MenuSlots<String> slots = new MenuSlots<>(54);

    @Test
    void storesAndReturnsItemsBySlot() {
        slots.set(10, "a");
        slots.set(53, "b");

        assertEquals("a", slots.get(10));
        assertEquals("b", slots.get(53));
    }

    @Test
    void emptyAndOutOfRangeSlotsReadAsNothing() {
        assertNull(slots.get(0));
        assertNull(slots.get(-1));
        assertNull(slots.get(54));
    }

    @Test
    void settingOutsideTheScreenIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> slots.set(54, "x"));
        assertThrows(IllegalArgumentException.class, () -> slots.set(-1, "x"));
    }

    @Test
    void settingASlotAgainReplacesIt() {
        slots.set(3, "old");
        slots.set(3, "new");

        assertEquals("new", slots.get(3));
    }

    @Test
    void clearEmptiesEverySlot() {
        slots.set(3, "a");

        slots.clear();

        assertNull(slots.get(3));
    }
}
