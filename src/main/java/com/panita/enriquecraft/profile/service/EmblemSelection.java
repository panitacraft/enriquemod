package com.panita.enriquecraft.profile.service;

import java.util.ArrayList;
import java.util.List;

/**
 * The editing of the list of emblems a player chose to show, as pure logic so every menu does it the same way. Lists are never
 * changed: each step gives a new one.
 */
public final class EmblemSelection {

    /** What toggling an emblem did. */
    public enum Change {
        ADDED,
        REMOVED,
        /** The list is already as long as it may be, so nothing changed. */
        FULL
    }

    /**
     * @param emblems the list after the step
     * @param change  what the step did
     */
    public record Toggled(List<String> emblems, Change change) {
    }

    private EmblemSelection() {
    }

    /** Puts an emblem away when it is shown, and adds it to the end when it is not and there is room. */
    public static Toggled toggle(List<String> chosen, String id, int max) {
        List<String> updated = new ArrayList<>(chosen);
        if (updated.remove(id)) {
            return new Toggled(updated, Change.REMOVED);
        }
        if (updated.size() >= max) {
            return new Toggled(updated, Change.FULL);
        }
        updated.add(id);
        return new Toggled(updated, Change.ADDED);
    }

    /** The dragged emblem takes the place of the one it is dropped on; the others keep their order. */
    public static List<String> move(List<String> chosen, String dragged, String onto) {
        int from = chosen.indexOf(dragged);
        int to = chosen.indexOf(onto);
        if (from < 0 || to < 0 || from == to) {
            return List.copyOf(chosen);
        }
        List<String> updated = new ArrayList<>(chosen);
        updated.add(to, updated.remove(from));
        return updated;
    }
}
