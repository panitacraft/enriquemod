package com.panita.enriquecraft.profile.data;

import net.minecraft.network.chat.TextColor;

/**
 * How an emblem looks to a player who sees only text: a character and a chat color. It is also what the title of its tooltip
 * and its name in a chest are drawn with.
 *
 * @param icon  one or two characters the game's font draws
 * @param color the chat color of the character and of the title: a name such as {@code gold} or a code such as {@code #ffaa00}
 */
public record EmblemLook(String icon, String color) {

    /** The color as the game uses it; white when the text is not a color, which the config rules out. */
    public TextColor textColor() {
        return TextColor.parseColor(color).result().orElse(TextColor.fromRgb(0xFFFFFF));
    }

    /** Whether the text is a color the game understands. */
    public static boolean isColor(String text) {
        return TextColor.parseColor(text).result().isPresent();
    }

    /** Whether the text is one or two characters that are neither blank nor control characters. */
    public static boolean isIcon(String text) {
        int points = text.codePointCount(0, text.length());
        return points >= 1 && points <= 2
                && text.codePoints().noneMatch(point -> Character.isISOControl(point) || Character.isWhitespace(point));
    }
}
