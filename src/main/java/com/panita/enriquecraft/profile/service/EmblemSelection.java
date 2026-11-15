package com.panita.enriquecraft.profile.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The editing of the list of emblems a player chose to show, as pure logic so every menu does it the same way. Lists are never
 * changed: each step gives a new one.
 */
public final class EmblemSelection {

    private EmblemSelection() {
    }

    /**
     * What choosing an emblem for a place does. A place that holds the emblem gives it up; a place that holds another emblem takes it,
     * and when the emblem was already shown in another place the two places trade; an empty place takes it at the end of the list, and
     * an emblem that was already shown moves to the end.
     *
     * @param chosen the ids shown now, in order
     * @param slot   the place the player is changing, counted from 0; a place past the end of the list is an empty one
     * @param max    the most emblems that can be shown
     */
    public static List<String> place(List<String> chosen, int slot, String id, int max) {
        List<String> updated = new ArrayList<>(chosen);
        int at = updated.indexOf(id);
        if (slot < updated.size()) {
            if (at == slot) {
                updated.remove(slot);
            } else if (at >= 0) {
                Collections.swap(updated, slot, at);
            } else {
                updated.set(slot, id);
            }
        } else if (at >= 0) {
            updated.add(updated.remove(at));
        } else if (updated.size() < max) {
            updated.add(id);
        }
        return updated;
    }
}
