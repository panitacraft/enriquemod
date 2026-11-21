package com.panita.enriquecraft.core.dialog;

import java.util.Map;

/**
 * What a player entered in a dialog, by field name. Every field of the dialog has a value, and asking for a
 * name the dialog does not have, or for the wrong kind, is a programming error rather than something a client
 * can cause.
 */
public final class DialogValues {

    private final Map<String, String> texts;
    private final Map<String, Boolean> toggles;
    private final Map<String, String> choices;

    DialogValues(Map<String, String> texts, Map<String, Boolean> toggles, Map<String, String> choices) {
        this.texts = Map.copyOf(texts);
        this.toggles = Map.copyOf(toggles);
        this.choices = Map.copyOf(choices);
    }

    /** The text of a text field, without control characters; empty when the player cleared it. */
    public String text(String name) {
        return require(texts, name);
    }

    public boolean toggle(String name) {
        return require(toggles, name);
    }

    /** The id of the option the player picked, always one the field offered. */
    public String choice(String name) {
        return require(choices, name);
    }

    private static <T> T require(Map<String, T> values, String name) {
        T value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("The dialog has no such field of that kind: " + name);
        }
        return value;
    }
}
