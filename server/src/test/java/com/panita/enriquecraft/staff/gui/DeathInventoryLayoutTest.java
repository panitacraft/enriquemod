package com.panita.enriquecraft.staff.gui;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathInventoryLayoutTest {

    @Test
    void mainInventoryFillsTheFirstThreeRowsInOrder() {
        assertEquals(0, DeathInventoryLayout.menuSlot(9));
        assertEquals(1, DeathInventoryLayout.menuSlot(10));
        assertEquals(26, DeathInventoryLayout.menuSlot(35));
    }

    @Test
    void hotbarIsTheRowUnderIt() {
        assertEquals(27, DeathInventoryLayout.menuSlot(0));
        assertEquals(35, DeathInventoryLayout.menuSlot(8));
    }

    @Test
    void armorReadsFromHeadToFeetAndTheOffhandFollows() {
        assertEquals(36, DeathInventoryLayout.menuSlot(39), "head");
        assertEquals(37, DeathInventoryLayout.menuSlot(38), "chest");
        assertEquals(38, DeathInventoryLayout.menuSlot(37), "legs");
        assertEquals(39, DeathInventoryLayout.menuSlot(36), "feet");
        assertEquals(41, DeathInventoryLayout.menuSlot(40), "offhand");
    }

    @Test
    void everyInventorySlotHasItsOwnSlotInsideTheItemArea() {
        Set<Integer> used = new HashSet<>();
        for (int slot = 0; slot <= 40; slot++) {
            int menuSlot = DeathInventoryLayout.menuSlot(slot);
            assertTrue(menuSlot >= 0 && menuSlot < 45, "slot " + slot + " -> " + menuSlot + " must stay above the button row");
            assertTrue(used.add(menuSlot), "slot " + slot + " -> " + menuSlot + " is used twice");
        }
        assertEquals(41, used.size());
    }

    @Test
    void slotsOutsideThePlayerInventoryAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DeathInventoryLayout.menuSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> DeathInventoryLayout.menuSlot(41));
        assertThrows(IllegalArgumentException.class, () -> DeathInventoryLayout.menuSlot(100));
    }
}
