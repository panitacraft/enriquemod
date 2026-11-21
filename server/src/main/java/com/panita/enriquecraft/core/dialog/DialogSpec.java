package com.panita.enriquecraft.core.dialog;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The description of a dialog: a title, some lines of text, inputs and buttons. It says nothing about how the
 * game draws it; {@link DialogFactory} turns it into a game dialog, and {@link DialogPayloadReader} uses it to
 * decide what a reply may contain.
 *
 * @param cancel the button that Escape presses, shown under the others; empty when Escape just closes the dialog
 * @param width  how wide the text, the inputs and the buttons are drawn, in pixels; buttons in columns share it
 * @param pinned whether the two buttons are the ones the game keeps at the bottom, side by side, instead of among the content
 */
public record DialogSpec(Component title, List<DialogBlock> body, List<DialogField> fields, List<DialogButton> buttons,
                         Optional<DialogButton> cancel, int columns, int width, boolean closeableWithEscape, boolean pinned) {

    private static final int MAX_FIELDS = 16;
    private static final int MAX_BUTTONS = 16;
    private static final int MAX_COLUMNS = 4;
    /** The width the game gives a dialog that does not ask for another. */
    public static final int DEFAULT_WIDTH = 200;
    private static final int MIN_WIDTH = 150;
    private static final int MAX_WIDTH = 400;

    public DialogSpec {
        body = List.copyOf(body);
        fields = List.copyOf(fields);
        buttons = List.copyOf(buttons);
        if (fields.size() > MAX_FIELDS || buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("A dialog takes at most " + MAX_FIELDS + " fields and " + MAX_BUTTONS + " buttons");
        }
        if (buttons.isEmpty() && cancel.isEmpty()) {
            throw new IllegalArgumentException("A dialog needs at least one button");
        }
        if (pinned && (buttons.size() != 2 || cancel.isPresent())) {
            throw new IllegalArgumentException("A pinned pair is exactly two buttons and no cancel button");
        }
        if (columns < 1 || columns > MAX_COLUMNS) {
            throw new IllegalArgumentException("columns must be between 1 and " + MAX_COLUMNS);
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException("width must be between " + MIN_WIDTH + " and " + MAX_WIDTH);
        }
        Set<String> names = new HashSet<>();
        for (DialogField field : fields) {
            if (!names.add(field.name())) {
                throw new IllegalArgumentException("The field name " + field.name() + " is used twice");
            }
        }
        Set<String> keys = new HashSet<>();
        for (DialogButton button : allButtons(buttons, cancel)) {
            if (!keys.add(button.key())) {
                throw new IllegalArgumentException("The button key " + button.key() + " is used twice");
            }
        }
    }

    public static Builder builder(Component title) {
        return new Builder(title);
    }

    /** The buttons the reply may name: the normal ones and the cancel button. */
    List<DialogButton> allButtons() {
        return allButtons(buttons, cancel);
    }

    private static List<DialogButton> allButtons(List<DialogButton> buttons, Optional<DialogButton> cancel) {
        List<DialogButton> all = new ArrayList<>(buttons);
        cancel.ifPresent(all::add);
        return all;
    }

    public static final class Builder {

        private final Component title;
        private final List<DialogBlock> body = new ArrayList<>();
        private final List<DialogField> fields = new ArrayList<>();
        private final List<DialogButton> buttons = new ArrayList<>();
        private Optional<DialogButton> cancel = Optional.empty();
        private int columns = 2;
        private int width = DEFAULT_WIDTH;
        private boolean closeableWithEscape = true;
        private boolean pinned;

        private Builder(Component title) {
            this.title = title;
        }

        public Builder body(Component text) {
            body.add(new DialogBlock.Message(text));
            return this;
        }

        /** How wide the dialog's content is (200 by default). */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder field(DialogField field) {
            fields.add(field);
            return this;
        }

        public Builder button(DialogButton button) {
            buttons.add(button);
            return this;
        }

        /**
         * Two buttons the game keeps at the bottom of the screen side by side, as it does the cancel button, and so they stay in
         * view however long the dialog is. Escape presses the second one, so make it the one that closes. A dialog with a pinned
         * pair has no other buttons.
         */
        public Builder pinned(DialogButton first, DialogButton second) {
            buttons.add(first);
            buttons.add(second);
            pinned = true;
            return this;
        }

        /** The button pressed by Escape. */
        public Builder cancel(DialogButton button) {
            cancel = Optional.of(button);
            return this;
        }

        /** How many buttons share a row (2 by default). */
        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        /** Makes Escape do nothing, so the player has to press a button. */
        public Builder requireChoice() {
            closeableWithEscape = false;
            return this;
        }

        public DialogSpec build() {
            return new DialogSpec(title, body, fields, buttons, cancel, columns, width, closeableWithEscape, pinned);
        }
    }
}
