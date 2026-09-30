package com.panita.enriquecraft.core.gui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuFrameTest {

    @Test
    void sixRowScreenHasATwentySixSlotBorderAndTwentyEightInside() {
        assertEquals(26, MenuFrame.borderSlots(6).size());
        assertEquals(28, MenuFrame.interiorSlots(6).size());
    }

    @Test
    void borderIsTheOuterEdge() {
        List<Integer> border = MenuFrame.borderSlots(6);

        assertTrue(border.containsAll(List.of(0, 1, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 52, 53)));
        assertTrue(border.subList(0, 9).equals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8)), "the first row is fully framed");
    }

    @Test
    void interiorStartsAtTheSecondRowSecondColumn() {
        List<Integer> interior = MenuFrame.interiorSlots(6);

        assertEquals(10, interior.get(0));
        assertEquals(16, interior.get(6));
        assertEquals(19, interior.get(7));
        assertEquals(43, interior.get(27));
    }

    @Test
    void borderAndInteriorPartitionTheScreen() {
        for (int rows = 1; rows <= 6; rows++) {
            List<Integer> all = new ArrayList<>(MenuFrame.borderSlots(rows));
            all.addAll(MenuFrame.interiorSlots(rows));
            all.sort(Integer::compare);

            assertEquals(rows * 9, all.size(), "rows " + rows);
            assertEquals(new ArrayList<>(all).stream().distinct().count(), all.size(), "no slot twice, rows " + rows);
        }
    }

    @Test
    void screensWithFewerThanThreeRowsHaveNoInterior() {
        assertTrue(MenuFrame.interiorSlots(1).isEmpty());
        assertTrue(MenuFrame.interiorSlots(2).isEmpty());
        assertEquals(7, MenuFrame.interiorSlots(3).size());
    }
}
