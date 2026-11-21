package com.panita.enriquecraft.core.ui;

import java.util.function.Consumer;

/**
 * What the server does with a dropdown: how many options it has, and the action to run.
 */
record UiSelectHandler(int optionCount, Consumer<UiSelect> action) {
}
