package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.network.UiElement;

import java.util.Map;
import java.util.function.Consumer;

/**
 * One description of a screen: what to show, and what each of its buttons does.
 *
 * @param nextId the first button id a later description of the same screen may use
 */
record UiLayout(UiElement root, Map<Integer, Consumer<UiClick>> handlers, int nextId) {
}
