package com.panita.enriquecraft.core.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * What is placed in each slot of a screen. Setting a slot outside the screen is a programming
 * error; reading one returns nothing.
 */
final class MenuSlots<T> {

    private final int size;
    private final Map<Integer, T> items = new HashMap<>();

    MenuSlots(int size) {
        this.size = size;
    }

    void set(int slot, T item) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Slot " + slot + " is outside a screen of " + size + " slots");
        }
        items.put(slot, item);
    }

    /** The item in a slot, or null when the slot is empty or outside the screen. */
    T get(int slot) {
        return items.get(slot);
    }

    void clear() {
        items.clear();
    }
}
