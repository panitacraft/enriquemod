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
     * Something the player can press. The icon may be empty, in which case no item is drawn. A button
     * with an icon but no label stands for the item itself, and shows its name and tooltip, followed by
     * the extra tooltip lines.
     *
     * @param id   identifies the button within one description; unique, assigned by the server
     * @param role what the button is for; navigation roles are placed by the client companion
     */
    record Button(int id, ButtonRole role, ItemStack icon, Component label, List<Component> tooltip) implements UiElement {
        public Button {
            tooltip = List.copyOf(tooltip);
        }
    }

    /**
     * A field the player types a value into; the value reaches the server when the player confirms it.
     *
     * @param id    identifies the field within one description; unique, assigned by the server
     * @param value what the field holds when it appears
     */
    record TextInput(int id, Component hint, String value, int maxLength) implements UiElement {
        public static final int MAX_LENGTH = 256;

        public TextInput {
            if (maxLength < 1 || maxLength > MAX_LENGTH || value.length() > maxLength) {
                throw new IllegalArgumentException("A field of up to " + maxLength + " characters cannot hold \"" + value + "\"");
            }
        }
    }

    /**
     * Where a list is: which page is showing out of how many. The client companion shows it next to the
     * page buttons; a chest has no place for it and ignores it.
     *
     * @param page  the page showing, counted from 1
     */
    record Page(int page, int pages) implements UiElement {
        public Page {
            if (page < 1 || pages < page) {
                throw new IllegalArgumentException("Page " + page + " of " + pages + " does not exist");
            }
        }
    }

    /**
     * One thing shown in full: its item, its name and some lines about it. The client companion draws the
     * item large with the text under it; a chest shows the item with the name and the lines as its tooltip.
     */
    record Detail(ItemStack icon, Component title, List<Component> lines) implements UiElement {
        public Detail {
            lines = List.copyOf(lines);
        }
    }

    /** An empty cell that keeps its neighbours in place. */
    record Spacer() implements UiElement {
    }
}
