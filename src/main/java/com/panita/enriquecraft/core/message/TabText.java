package com.panita.enriquecraft.core.message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * A finished text reduced to what a tab list mod can read: literal text with a color and the plain styles, nothing else. Such a mod
 * (TAB) turns a component into its own string by reading only literal text, and prints the raw contents of anything else, such as
 * the translation behind an image. Hover, click and fonts are dropped, since the tab list cannot use them. Every part gets an explicit
 * color, because that mod writes none of its own and a part without one would take the color of the part before it.
 */
public final class TabText {

    private static final TextColor DEFAULT_COLOR = TextColor.fromLegacyFormat(ChatFormatting.WHITE);

    private TabText() {
    }

    public static Component plain(Component text) {
        MutableComponent plain = Component.empty().withStyle(Style.EMPTY.withColor(DEFAULT_COLOR));
        for (Component part : text.toFlatList()) {
            Style style = part.getStyle();
            TextColor color = style.getColor() == null ? DEFAULT_COLOR : style.getColor();
            plain.append(Component.literal(part.getString()).withStyle(Style.EMPTY.withColor(color)
                    .withBold(style.isBold() ? Boolean.TRUE : null)
                    .withItalic(style.isItalic() ? Boolean.TRUE : null)
                    .withUnderlined(style.isUnderlined() ? Boolean.TRUE : null)
                    .withStrikethrough(style.isStrikethrough() ? Boolean.TRUE : null)
                    .withObfuscated(style.isObfuscated() ? Boolean.TRUE : null)));
        }
        return plain;
    }
}
