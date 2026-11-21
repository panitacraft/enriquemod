package com.panita.enriquecraft.core.dialog;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads what a client sent when it pressed a dialog button. The client is untrusted: it may send anything, so the
 * reply is accepted only when it is exactly what the dialog could have produced. Names the dialog did not declare,
 * a wrong token, a wrong kind of value, a text over its limit or an option that was not offered are all refused,
 * and the texts are cleaned of what the player could never have typed in chat.
 */
final class DialogPayloadReader {

    /** A token is far shorter than this; the bound keeps a hostile string from being compared at length. */
    private static final int MAX_TOKEN_LENGTH = 64;
    private static final int MAX_BUTTON_LENGTH = 32;

    sealed interface Result {
    }

    record Accepted(String button, DialogValues values) implements Result {
    }

    /** A reply to a dialog that is gone, which happens in normal play when a dialog is replaced as the player presses a button. */
    record Stale() implements Result {
    }

    /** The reason is for the console only; it is English and never shown to a player. */
    record Rejected(String reason) implements Result {
    }

    private DialogPayloadReader() {
    }

    static Result read(DialogSpec spec, String token, Optional<Tag> payload) {
        if (payload.isEmpty() || !(payload.get() instanceof CompoundTag compound)) {
            return new Rejected("the payload is not a compound");
        }
        Optional<String> sentToken = string(compound, DialogNames.TOKEN_KEY, MAX_TOKEN_LENGTH);
        if (sentToken.isEmpty() || !sentToken.get().equals(token)) {
            return new Stale();
        }
        // A real reply has the token, the button and one entry per field, nothing more.
        if (compound.size() != spec.fields().size() + 2) {
            return new Rejected("the payload has " + compound.size() + " entries, not " + (spec.fields().size() + 2));
        }
        Set<String> allowed = new HashSet<>(Set.of(DialogNames.TOKEN_KEY, DialogNames.BUTTON_KEY));
        spec.fields().forEach(field -> allowed.add(field.name()));
        for (String key : compound.keySet()) {
            if (!allowed.contains(key)) {
                return new Rejected("the payload has an undeclared entry");
            }
        }

        Optional<String> button = string(compound, DialogNames.BUTTON_KEY, MAX_BUTTON_LENGTH);
        if (button.isEmpty() || spec.allButtons().stream().noneMatch(declared -> declared.key().equals(button.get()))) {
            return new Rejected("the button is not one of the dialog");
        }

        Map<String, String> texts = new HashMap<>();
        Map<String, Boolean> toggles = new HashMap<>();
        Map<String, String> choices = new HashMap<>();
        for (DialogField field : spec.fields()) {
            Tag value = compound.get(field.name());
            String problem = switch (field) {
                case DialogField.Text text -> readText(text, value, texts);
                case DialogField.Toggle toggle -> readToggle(toggle, value, toggles);
                case DialogField.Choice choice -> readChoice(choice, value, choices);
            };
            if (problem != null) {
                return new Rejected(problem + " (" + field.name() + ")");
            }
        }
        return new Accepted(button.get(), new DialogValues(texts, toggles, choices));
    }

    private static Optional<String> string(CompoundTag compound, String key, int maxLength) {
        return compound.get(key) instanceof StringTag tag && tag.value().length() <= maxLength
                ? Optional.of(tag.value())
                : Optional.empty();
    }

    private static String readText(DialogField.Text field, Tag value, Map<String, String> into) {
        if (!(value instanceof StringTag tag)) {
            return "a text field did not get a string";
        }
        String raw = tag.value();
        if (raw.length() > field.maxLength()) {
            return "a text is over its limit";
        }
        if (field.multiline() && raw.chars().filter(c -> c == '\n').count() >= field.lines()) {
            return "a text has more lines than its field";
        }
        into.put(field.name(), clean(raw, field.multiline()));
        return null;
    }

    private static String readToggle(DialogField.Toggle field, Tag value, Map<String, Boolean> into) {
        if (!(value instanceof ByteTag tag) || (tag.byteValue() != 0 && tag.byteValue() != 1)) {
            return "a toggle did not get a boolean";
        }
        into.put(field.name(), tag.byteValue() == 1);
        return null;
    }

    private static String readChoice(DialogField.Choice field, Tag value, Map<String, String> into) {
        if (!(value instanceof StringTag tag) || !field.offers(tag.value())) {
            return "a choice got an option it does not offer";
        }
        into.put(field.name(), tag.value());
        return null;
    }

    /**
     * Removes what no player types into a form: control characters (a text area keeps its newlines), format
     * characters and the section sign, which would start a color in any text the server later builds.
     */
    static String clean(String text, boolean multiline) {
        StringBuilder cleaned = new StringBuilder(text.length());
        text.codePoints()
                .filter(point -> (point == '\n' && multiline)
                        || (point != '§' && !Character.isISOControl(point) && Character.getType(point) != Character.FORMAT))
                .forEach(cleaned::appendCodePoint);
        return cleaned.toString();
    }
}
