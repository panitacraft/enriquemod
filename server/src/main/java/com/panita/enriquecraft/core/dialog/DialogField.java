package com.panita.enriquecraft.core.dialog;

import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An input of a dialog. The name is the key under which the client sends the value back, so it is also
 * what the reader checks the reply against: a reply may only carry the names its dialog declared.
 */
public sealed interface DialogField {

    /** The longest text a field may take; the packet that carries the reply is limited too. */
    int MAX_TEXT_LENGTH = 1024;
    int MAX_MULTILINE_LINES = 16;

    String name();

    Component label();

    /**
     * A text box.
     *
     * @param lines 0 for a single line, otherwise the number of lines of a text area (the only kind that takes newlines)
     */
    record Text(String name, Component label, String initial, int maxLength, int lines) implements DialogField {
        public Text {
            DialogNames.require(name);
            if (maxLength < 1 || maxLength > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("maxLength must be between 1 and " + MAX_TEXT_LENGTH);
            }
            if (lines < 0 || lines > MAX_MULTILINE_LINES) {
                throw new IllegalArgumentException("lines must be between 0 and " + MAX_MULTILINE_LINES);
            }
            if (initial.length() > maxLength) {
                throw new IllegalArgumentException("The initial text of " + name + " is longer than its limit");
            }
        }

        public boolean multiline() {
            return lines > 0;
        }
    }

    /** A checkbox. */
    record Toggle(String name, Component label, boolean initial) implements DialogField {
        public Toggle {
            DialogNames.require(name);
        }
    }

    /** One choice among a few options, shown as a button that cycles through them. */
    record Choice(String name, Component label, List<Option> options, String initial) implements DialogField {

        public Choice {
            DialogNames.require(name);
            options = List.copyOf(options);
            if (options.isEmpty()) {
                throw new IllegalArgumentException(name + " needs at least one option");
            }
            Set<String> ids = new HashSet<>();
            for (Option option : options) {
                if (!ids.add(option.id())) {
                    throw new IllegalArgumentException(name + " repeats the option " + option.id());
                }
            }
            if (!ids.contains(initial)) {
                throw new IllegalArgumentException("The initial option of " + name + " is not one of its options");
            }
        }

        public boolean offers(String id) {
            return options.stream().anyMatch(option -> option.id().equals(id));
        }
    }

    /** An option of a {@link Choice}: the id the server gets back and what the player reads. */
    record Option(String id, Component label) {
        private static final int MAX_ID_LENGTH = 64;

        public Option {
            if (id.isEmpty() || id.length() > MAX_ID_LENGTH || id.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("An option id must be 1 to " + MAX_ID_LENGTH + " characters without control characters");
            }
        }
    }
}
