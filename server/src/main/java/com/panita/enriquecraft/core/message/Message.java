package com.panita.enriquecraft.core.message;

import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable description of a player-facing message: a template, its level, and its named arguments.
 * <p>
 * Templates use text tags (for example {@code <bold>}), legacy color codes (for example
 * {@code &c}), server placeholders (for example {@code %player:name%}, only where the Messenger
 * method resolves them) and named arguments written as {@code {name}}. Arguments are inserted as
 * finished components, so their content is never parsed: pass untrusted text through
 * {@code with}, never by building the template with string concatenation.
 *
 * @param template  the raw template
 * @param level     the visual category of the message
 * @param hasPrefix whether the mod prefix is prepended
 * @param arguments values for the {@code {name}} arguments
 */
public record Message(String template, MessageLevel level, boolean hasPrefix, Map<String, Component> arguments) {

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
        Map<String, Component> updated = new HashMap<>(arguments);
        updated.put(name, value);
        return new Message(template, level, hasPrefix, updated);
    }

    public Message with(String name, String value) {
        return with(name, Component.literal(value));
    }

    public Message with(String name, int value) {
        return with(name, String.valueOf(value));
    }
}
