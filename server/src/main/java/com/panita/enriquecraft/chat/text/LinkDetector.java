package com.panita.enriquecraft.chat.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Turns web addresses in a text into links. Only {@code http} and {@code https} addresses are links, and a link only
 * ever opens an address: the client asks the player before it does.
 */
public final class LinkDetector {

    // The end of a sentence is not part of the address: "see https://a.b/c." links to https://a.b/c.
    private static final Pattern ADDRESS = Pattern.compile("(?i)\\bhttps?://[^\\s<>\"]*[^\\s<>\".,;:!?)\\]]");

    private final Supplier<Component> tooltip;

    /** @param tooltip what shows when the player points at a link */
    public LinkDetector(Supplier<Component> tooltip) {
        this.tooltip = tooltip;
    }

    public Component apply(Component component) {
        return TextRewriter.rewrite(component, ADDRESS, this::link, part -> part.getStyle().getClickEvent() != null);
    }

    private Component link(String address) {
        URI uri;
        try {
            uri = URI.create(address);
        } catch (IllegalArgumentException e) {
            return Component.literal(address);
        }
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(uri))
                .withHoverEvent(new HoverEvent.ShowText(tooltip.get()));
        return Component.literal(address).setStyle(style);
    }
}
