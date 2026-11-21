package com.panita.enriquecraft.core.ui;

import java.util.function.Consumer;

/**
 * What the server does with a text field: the longest value it accepts, whether the value may span several
 * lines (only a text area's may), and the action to run.
 */
record UiInputHandler(int maxLength, boolean multiline, Consumer<UiSubmit> action) {

    /** Whether a typed value is one this field accepts: short enough and free of control characters. */
    boolean accepts(String text) {
        return text.length() <= maxLength
                && text.chars().noneMatch(c -> Character.isISOControl(c) && !(multiline && c == '\n'));
    }
}
