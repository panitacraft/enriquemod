package com.panita.enriquecraft.chat.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The color an AFK player's rank and name take: read from the setting, written as a legacy color code ({@code &7}) or a hex color
 * ({@code #aaaaaa} or {@code &#aaaaaa}), and painted over a finished text without touching its other styles.
 */
public final class AfkColor {

    public static final String DEFAULT = "&7";
    public static final String RULE = "must be a color code such as &7, or a hex color such as #aaaaaa";

    private static final Pattern LEGACY = Pattern.compile("&([0-9a-fA-F])");
    private static final Pattern HEX = Pattern.compile("&?#([0-9a-fA-F]{6})");

    private AfkColor() {
    }

    public static boolean isValid(String text) {
        return parse(text).isPresent();
    }

    public static Optional<TextColor> parse(String text) {
        String value = text.strip();
        var legacy = LEGACY.matcher(value);
        if (legacy.matches()) {
            ChatFormatting formatting = ChatFormatting.getByCode(Character.toLowerCase(legacy.group(1).charAt(0)));
            return Optional.ofNullable(formatting).map(TextColor::fromLegacyFormat);
        }
        var hex = HEX.matcher(value);
        if (hex.matches()) {
            return Optional.of(TextColor.fromRgb(Integer.parseInt(hex.group(1), 16)));
        }
        return Optional.empty();
    }

    /**
     * The text in one color. Every part loses the color it had, and keeps its other styles (bold, hover, click). Only text is kept:
     * a part that draws something else, such as the image of a rank, becomes the text it falls back to.
     */
    public static Component paint(Component text, TextColor color) {
        MutableComponent painted = Component.empty();
        for (Component part : text.toFlatList()) {
            painted.append(part.copy().withStyle(style -> style.withColor(color)));
        }
        return painted;
    }
}
