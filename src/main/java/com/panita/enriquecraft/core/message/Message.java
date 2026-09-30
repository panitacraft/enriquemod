package com.panita.enriquecraft.core.message;

import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable description of a player-facing message: a template, its level, and its named arguments.
 * <p>
 * Templates use text tags (for example {@code <bold>}), server placeholders (for example
 * {@code %player:name%}) and named arguments written as {@code {name}}. Arguments added with
 * {@code with} are inserted as finished components, so their content is never parsed as tags.
 * Use {@link #withMarkup(String, String)} for trusted input that may contain formatting.
 *
 * @param template  the raw template
 * @param level     the visual category of the message
 * @param hasPrefix whether the mod prefix is prepended
 * @param arguments values for the {@code {name}} arguments
 */
public record Message(String template, MessageLevel level, boolean hasPrefix, Map<String, MessageArgument> arguments) {

    public Message {
        arguments = Map.copyOf(arguments);
    }

    public static Message plain(String template) {
        return of(template, MessageLevel.PLAIN);
    }

    public static Message info(String template) {
        return of(template, MessageLevel.INFO);
    }

    public static Message success(String template) {
        return of(template, MessageLevel.SUCCESS);
    }

    public static Message warning(String template) {
        return of(template, MessageLevel.WARNING);
    }

    public static Message error(String template) {
        return of(template, MessageLevel.ERROR);
    }

    private static Message of(String template, MessageLevel level) {
        return new Message(template, level, false, Map.of());
    }

    public Message prefixed() {
        return new Message(template, level, true, arguments);
    }

    public Message with(String name, Component value) {
        return withArgument(name, new MessageArgument.Text(value));
    }

    public Message with(String name, String value) {
        return with(name, Component.literal(value));
    }

    public Message with(String name, int value) {
        return with(name, String.valueOf(value));
    }

    /**
     * Adds an argument whose text is parsed for text tags and legacy color codes.
     */
    public Message withMarkup(String name, String raw) {
        return withArgument(name, new MessageArgument.Markup(raw));
    }

    private Message withArgument(String name, MessageArgument argument) {
        Map<String, MessageArgument> updated = new HashMap<>(arguments);
        updated.put(name, argument);
        return new Message(template, level, hasPrefix, updated);
    }
}
