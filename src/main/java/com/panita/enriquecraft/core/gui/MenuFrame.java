package com.panita.enriquecraft.core.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot arithmetic for the decorative frame of a chest screen, which is nine slots wide.
 */
public final class MenuFrame {

    public static final int COLUMNS = 9;

    private MenuFrame() {
    }

    /** The slots on the outer edge of a screen with the given number of rows, in ascending order. */
    public static List<Integer> borderSlots(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < rows * COLUMNS; slot++) {
            if (isBorder(slot, rows)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    /** The slots inside the frame, in ascending order; empty when the screen has fewer than three rows. */
    public static List<Integer> interiorSlots(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < rows * COLUMNS; slot++) {
            if (!isBorder(slot, rows)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static boolean isBorder(int slot, int rows) {
        int row = slot / COLUMNS;
        int column = slot % COLUMNS;
        return row == 0 || row == rows - 1 || column == 0 || column == COLUMNS - 1;
    }
}
