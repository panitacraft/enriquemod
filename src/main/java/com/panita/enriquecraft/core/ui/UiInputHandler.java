package com.panita.enriquecraft.core.ui;

import java.util.function.Consumer;

/**
 * What the server does with a text field: the longest value it accepts, and the action to run.
 */
record UiInputHandler(int maxLength, Consumer<UiSubmit> action) {
}
