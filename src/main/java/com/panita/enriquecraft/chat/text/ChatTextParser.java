package com.panita.enriquecraft.chat.text;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Turns what a player typed into a component, reading only what their capabilities allow. Everything else stays
 * as the literal text they wrote. This is the only way player text becomes a component: the parser the
 * administrator uses for the mod's own texts must never see it.
 */
public final class ChatTextParser {

    private static final List<String> CLICK_TAGS = List.of("open_url", "suggest_command", "copy_to_clipboard");
    private static final Set<ChatCapability> TAG_CAPABILITIES = EnumSet.of(ChatCapability.COLOR, ChatCapability.FORMAT,
            ChatCapability.GRADIENT, ChatCapability.HOVER, ChatCapability.CLICK);

    // The game declares the sixteen colors first, then the styles, then the reset.
    private static final Set<ChatFormatting> COLORS = EnumSet.range(ChatFormatting.BLACK, ChatFormatting.WHITE);
    private static final Set<ChatFormatting> FORMATS = EnumSet.range(ChatFormatting.OBFUSCATED, ChatFormatting.ITALIC);

    private final EmojiCatalog emojis;
    private final LinkDetector links;
    private final Map<Set<ChatCapability>, NodeParser> parsers = new ConcurrentHashMap<>();

    public ChatTextParser(EmojiCatalog emojis, LinkDetector links) {
        this.emojis = emojis;
        this.links = links;
    }

    /**
     * @param raw          what the player typed
     * @param capabilities what they may do with it
     */
    public Component parse(String raw, Set<ChatCapability> capabilities) {
        Set<ChatCapability> allowed = capabilities.isEmpty() ? EnumSet.noneOf(ChatCapability.class) : EnumSet.copyOf(capabilities);
        String text = sanitize(raw);
        Component component = allowed.stream().anyMatch(TAG_CAPABILITIES::contains)
                ? parsers.computeIfAbsent(allowed, ChatTextParser::build).parseComponent(text, ParserContext.of())
                : Component.literal(text);
        if (allowed.contains(ChatCapability.LINKS)) {
            component = links.apply(component);
        }
        if (allowed.contains(ChatCapability.EMOJI)) {
            component = emojis.apply(component);
        }
        return component;
    }

    /** Control characters have no place in chat, and a section sign would be read by the client as a color code. */
    static String sanitize(String raw) {
        StringBuilder text = new StringBuilder(raw.length());
        raw.codePoints()
                .filter(character -> character != '§' && !Character.isISOControl(character))
                .forEach(text::appendCodePoint);
        return text.toString().strip();
    }

    private static NodeParser build(Set<ChatCapability> capabilities) {
        TagRegistry.Builder tags = TagRegistry.builder();
        for (ChatCapability capability : capabilities) {
            switch (capability) {
                case COLOR -> tags(TagRegistry.DEFAULT, tag -> tag.type().equals("color") && tag.userSafe(), tags);
                case FORMAT -> tags(TagRegistry.DEFAULT, tag -> tag.type().equals("formatting") && tag.userSafe(), tags);
                case GRADIENT -> tags(TagRegistry.DEFAULT, tag -> tag.type().equals("gradient"), tags);
                case HOVER -> tags(TagRegistry.DEFAULT, tag -> tag.name().equals("hover"), tags);
                case CLICK -> tags(TagRegistry.DEFAULT, tag -> CLICK_TAGS.contains(tag.name()), tags);
                case LINKS, EMOJI -> {
                }
            }
        }
        ParserBuilder builder = ParserBuilder.of().customTagRegistry(tags.build()).simplifiedTextFormat();
        Set<ChatFormatting> codes = legacyCodes(capabilities);
        if (!codes.isEmpty()) {
            // The first argument lets a color be written as a hexadecimal number, &#rrggbb.
            builder.legacy(capabilities.contains(ChatCapability.COLOR), codes);
        }
        return builder.build();
    }

    private static void tags(TagRegistry from, Predicate<TextTag> wanted, TagRegistry.Builder into) {
        from.getTags().stream().filter(wanted).forEach(into::add);
    }

    private static Set<ChatFormatting> legacyCodes(Set<ChatCapability> capabilities) {
        Set<ChatFormatting> codes = EnumSet.noneOf(ChatFormatting.class);
        if (capabilities.contains(ChatCapability.COLOR)) {
            codes.addAll(COLORS);
        }
        if (capabilities.contains(ChatCapability.FORMAT)) {
            codes.addAll(FORMATS);
        }
        if (!codes.isEmpty()) {
            codes.add(ChatFormatting.RESET);
        }
        return codes;
    }
}
