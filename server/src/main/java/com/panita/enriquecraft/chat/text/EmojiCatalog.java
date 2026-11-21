package com.panita.enriquecraft.chat.text;

import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The emojis the administrator configured: a trigger such as {@code :heart:} or {@code <3} and the text that stands for
 * it. Triggers are found literally, the longest first, and what replaces them is read as the administrator wrote it,
 * never as player text. The table is read when it is needed, so a reload is picked up; what is built from it is kept
 * until the table changes.
 */
public final class EmojiCatalog {

    private final Supplier<Map<String, String>> table;
    private final Function<String, Component> parseReplacement;

    private Map<String, String> builtFrom;
    private Pattern triggers;
    private Map<String, Component> replacements = Map.of();

    /**
     * @param table            the configured triggers and their replacements
     * @param parseReplacement turns the administrator's text for a replacement into a component
     */
    public EmojiCatalog(Supplier<Map<String, String>> table, Function<String, Component> parseReplacement) {
        this.table = table;
        this.parseReplacement = parseReplacement;
    }

    /** The component with every trigger in its text replaced; parts that can be clicked are left as they are. */
    public Component apply(Component component) {
        refresh();
        if (triggers == null) {
            return component;
        }
        return TextRewriter.rewrite(component, triggers, replacements::get, part -> part.getStyle().getClickEvent() != null);
    }

    private void refresh() {
        Map<String, String> current = table.get();
        // A reload gives a new table, an unchanged file gives the same one.
        if (current == builtFrom) {
            return;
        }
        builtFrom = current;
        if (current.isEmpty()) {
            triggers = null;
            replacements = Map.of();
            return;
        }
        Map<String, Component> built = new HashMap<>();
        current.forEach((trigger, replacement) -> built.put(trigger, parseReplacement.apply(replacement)));
        replacements = built;
        String alternatives = current.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
                .map(Pattern::quote)
                .reduce((left, right) -> left + "|" + right)
                .orElseThrow();
        triggers = Pattern.compile(alternatives);
    }
}
