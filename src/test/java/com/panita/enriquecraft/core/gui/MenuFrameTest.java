package com.panita.enriquecraft.core.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuFrameTest {

    @Test
    void sixRowScreenHasATwentySixSlotBorder() {
        assertEquals(26, MenuFrame.borderSlots(6).size());
    }

    @Test
    void borderIsTheOuterEdge() {
        List<Integer> border = MenuFrame.borderSlots(6);

        assertTrue(border.containsAll(List.of(0, 1, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 52, 53)));
        assertTrue(border.subList(0, 9).equals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8)), "the first row is fully framed");
    }

    @Test
    void borderLeavesTheInteriorFree() {
        List<Integer> border = MenuFrame.borderSlots(6);

        assertTrue(List.of(10, 16, 19, 43).stream().noneMatch(border::contains));
    }

    @Test
    void screensWithFewerThanThreeRowsAreAllBorder() {
        assertEquals(9, MenuFrame.borderSlots(1).size());
        assertEquals(18, MenuFrame.borderSlots(2).size());
        assertEquals(20, MenuFrame.borderSlots(3).size());
    }
}
