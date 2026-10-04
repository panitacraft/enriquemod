package com.panita.enriquecraft.core.network;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One piece of a server-described screen. The tree is pure description: it holds no behavior, so it
 * can be sent to the client companion or laid out as a vanilla chest. What a button does stays on
 * the server, found again through the button's {@code id}.
 */
public sealed interface UiElement {

    /** Stacks its children vertically. */
    record Column(List<UiElement> children) implements UiElement {
        public Column {
            children = List.copyOf(children);
        }
    }

    /** Places its children side by side. */
    record Row(List<UiElement> children) implements UiElement {
        public Row {
            children = List.copyOf(children);
        }
    }

    /**
     * A grid of {@code columns} by {@code rows} cells, filled left to right and top to bottom. It may
     * hold fewer children than cells; the rest stay empty.
     */
    record Grid(int columns, int rows, List<UiElement> children) implements UiElement {
        public Grid {
            if (columns < 1 || rows < 1 || children.size() > columns * rows) {
                throw new IllegalArgumentException(
                        children.size() + " children do not fit a grid of " + columns + " by " + rows);
            }
            children = List.copyOf(children);
        }
    }

    /** A line of text. */
    record Label(Component text) implements UiElement {
    }

    /**
     * Something the player can press. The icon may be empty, in which case the client draws no item.
     *
     * @param id identifies the button within one description; unique, assigned by the server
     */
    record Button(int id, ItemStack icon, Component label, List<Component> tooltip) implements UiElement {
        public Button {
            tooltip = List.copyOf(tooltip);
        }
    }

    /** An empty cell that keeps its neighbours in place. */
    record Spacer() implements UiElement {
    }
}
