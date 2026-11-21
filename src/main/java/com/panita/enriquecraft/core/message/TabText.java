package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.art.InlineArts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * A finished text reduced to what a tab list mod can read: literal text with a color and the plain styles, nothing else. Such a mod
 * (TAB) turns a component into its own string by reading only literal text, and prints the raw contents of anything else, such as
 * the translation behind an image. Hover, click and fonts are dropped, since the tab list cannot use them. Every part gets an explicit
 * color, because that mod writes none of its own and a part without one would take the color of the part before it.
 * <p>
 * An image is its text, the first argument of its translation. The server may know the companion's language file, which makes the
 * translation pick the image's glyph instead, so the first argument is taken by hand and never through the language.
 */
public final class TabText {

    private static final TextColor DEFAULT_COLOR = TextColor.fromLegacyFormat(ChatFormatting.WHITE);

    private TabText() {
    }

    public static Component plain(Component text) {
        MutableComponent plain = Component.empty().withStyle(Style.EMPTY.withColor(DEFAULT_COLOR));
        append(plain, text, Style.EMPTY);
        return plain;
    }

    private static void append(MutableComponent plain, Component part, Style inherited) {
        Style style = part.getStyle().applyTo(inherited);
        if (part.getContents() instanceof TranslatableContents translation && InlineArts.KEY.equals(translation.getKey())) {
            if (translation.getArgs().length > 0 && translation.getArgs()[0] instanceof Component shown) {
                append(plain, shown, style);
            }
        } else {
            for (Component flat : MutableComponent.create(part.getContents()).toFlatList(style)) {
                addLiteral(plain, flat);
            }
        }
        for (Component sibling : part.getSiblings()) {
            append(plain, sibling, style);
        }
    }

    private static void addLiteral(MutableComponent plain, Component flat) {
        String text = flat.getString();
        if (text.isEmpty()) {
            return;
        }
        Style style = flat.getStyle();
        TextColor color = style.getColor() == null ? DEFAULT_COLOR : style.getColor();
        plain.append(Component.literal(text).withStyle(Style.EMPTY.withColor(color)
                .withBold(style.isBold() ? Boolean.TRUE : null)
                .withItalic(style.isItalic() ? Boolean.TRUE : null)
                .withUnderlined(style.isUnderlined() ? Boolean.TRUE : null)
                .withStrikethrough(style.isStrikethrough() ? Boolean.TRUE : null)
                .withObfuscated(style.isObfuscated() ? Boolean.TRUE : null)));
    }
}
