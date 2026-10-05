package com.panita.enriquecraft.core.network;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

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
     * @param id    identifies the button within one description; unique, assigned by the server
     * @param role  what the button is for; navigation roles are placed by the client companion
     * @param badge a short mark drawn over the item, such as a check; empty for none
     * @param tint  an RGB color the client companion gives an action instead of the one its role implies, so a
     *              kind of action can have an identity of its own, or {@link #NO_TINT}
     * @param draggable whether the client companion lets the player drag the button onto another draggable one,
     *                  which it reports as a drop; a chest has no dragging and ignores the flag
     */
    record Button(int id, ButtonRole role, ItemStack icon, Component label, List<Component> tooltip, String badge,
                  int tint, boolean draggable) implements UiElement {
        public static final int MAX_BADGE_LENGTH = 4;
        public static final int NO_TINT = 0;
        public static final int MAX_TINT = 0xFFFFFF;

        public Button {
            tooltip = List.copyOf(tooltip);
            if (badge.length() > MAX_BADGE_LENGTH) {
                throw new IllegalArgumentException("A badge is at most " + MAX_BADGE_LENGTH + " characters: " + badge);
            }
            if (tint < NO_TINT || tint > MAX_TINT) {
                throw new IllegalArgumentException("A tint is an RGB color, not " + tint);
            }
        }

        /** A button that is not draggable. */
        public Button(int id, ButtonRole role, ItemStack icon, Component label, List<Component> tooltip, String badge,
                      int tint) {
            this(id, role, icon, label, tooltip, badge, tint, false);
        }

        /** A button without a tint. */
        public Button(int id, ButtonRole role, ItemStack icon, Component label, List<Component> tooltip, String badge) {
            this(id, role, icon, label, tooltip, badge, NO_TINT);
        }

        /** The same button, which can be dragged. */
        public Button asDraggable() {
            return new Button(id, role, icon, label, tooltip, badge, tint, true);
        }

        /** A button without a badge. */
        public Button(int id, ButtonRole role, ItemStack icon, Component label, List<Component> tooltip) {
            this(id, role, icon, label, tooltip, "");
        }
    }

    /**
     * A choice among a few options, shown as a button that opens a list. Choosing one tells the server
     * its position; what to do about it is up to the server, which sends the screen again. A chest shows
     * an item that moves to the next option on a left click and to the previous one on a right click.
     *
     * @param id       identifies the dropdown within one description; unique, assigned by the server
     * @param label    names what is being chosen
     * @param selected the position of the option chosen now
     */
    record Dropdown(int id, Component label, List<Component> options, int selected) implements UiElement {
        public static final int MAX_OPTIONS = 32;

        public Dropdown {
            options = List.copyOf(options);
            if (options.isEmpty() || options.size() > MAX_OPTIONS || selected < 0 || selected >= options.size()) {
                throw new IllegalArgumentException("Option " + selected + " does not exist among " + options.size());
            }
        }
    }

    /**
     * Content that may be taller than the space it gets, shown with a scroll bar by the client companion.
     * A chest cannot scroll, so it shows the content as if it were not wrapped.
     *
     * @param maxHeight the most height, in GUI pixels, the content takes before it scrolls
     */
    record Scroll(UiElement content, int maxHeight) implements UiElement {
        public Scroll {
            if (maxHeight < 1) {
                throw new IllegalArgumentException("A scroll area needs a positive height");
            }
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
     * The item can be pressable, for example to change it.
     *
     * @param iconId      identifies the pressable item within one description, or {@link #NOT_PRESSABLE}
     * @param iconTooltip what pressing the item does, shown when hovering it
     * @param editableTitle when present, the client companion lets the player edit the title in place and
     *                      confirm it with a check; a chest ignores it, so a menu offers a field of its own there
     *                      (an empty title is simply not drawn)
     */
    record Detail(ItemStack icon, Component title, List<Component> lines, int iconId, List<Component> iconTooltip,
                  Optional<TextInput> editableTitle) implements UiElement {
        public static final int NOT_PRESSABLE = -1;

        public Detail {
            lines = List.copyOf(lines);
            iconTooltip = List.copyOf(iconTooltip);
        }

        /** A detail with a pressable item and a fixed title. */
        public Detail(ItemStack icon, Component title, List<Component> lines, int iconId, List<Component> iconTooltip) {
            this(icon, title, lines, iconId, iconTooltip, Optional.empty());
        }

        /** A detail whose item cannot be pressed. */
        public Detail(ItemStack icon, Component title, List<Component> lines) {
            this(icon, title, lines, NOT_PRESSABLE, List.of());
        }

        /** The same detail with a title the player can edit. */
        public Detail withEditableTitle(TextInput input) {
            return new Detail(icon, title, lines, iconId, iconTooltip, Optional.of(input));
        }
    }

    /** A thin line across the screen that separates what is above it from what is below. A chest has no place for it. */
    record Divider() implements UiElement {
    }

    /** An empty cell that keeps its neighbours in place. */
    record Spacer() implements UiElement {
    }
}
