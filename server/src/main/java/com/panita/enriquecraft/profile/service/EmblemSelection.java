package com.panita.enriquecraft.profile.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The editing of the emblems a player shows, as pure logic so every menu does it the same way. Lists are never changed: each step
 * gives a new one. A list holds one id for each place, in order, and an empty string for a place with nothing in it.
 */
public final class EmblemSelection {

    private EmblemSelection() {
    }

    /**
     * What choosing an emblem for a place does: a place that holds the emblem gives it up; a place takes any other emblem, and when
     * that emblem was shown in another place, the two places trade (an empty place included).
     *
     * @param placed the id in each place now, empty strings for empty places
     * @param slot   the place the player is changing, counted from 0
     * @param max    how many places there are
     * @return the list to keep: one id per place, without the empty places at the end
     */
    public static List<String> place(List<String> placed, int slot, String id, int max) {
        List<String> updated = new ArrayList<>(placed);
        while (updated.size() < max) {
            updated.add("");
        }
        int at = updated.indexOf(id);
        if (slot < 0 || slot >= updated.size()) {
            return trimmed(updated);
        }
        if (at == slot) {
            updated.set(slot, "");
        } else if (at >= 0) {
            Collections.swap(updated, slot, at);
        } else {
            updated.set(slot, id);
        }
        return trimmed(updated);
    }

    private static List<String> trimmed(List<String> places) {
        int end = places.size();
        while (end > 0 && places.get(end - 1).isEmpty()) {
            end--;
        }
        return List.copyOf(places.subList(0, end));
    }
}
