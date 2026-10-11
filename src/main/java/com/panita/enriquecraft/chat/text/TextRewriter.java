package com.panita.enriquecraft.chat.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces pieces of the literal text inside a component, keeping its styles. The pieces are found in each
 * run of text on its own, so a match never reaches across two styled parts.
 */
final class TextRewriter {

    private TextRewriter() {
    }

    /**
     * @param replacement makes the component that stands for a matched text
     * @param skip        parts that are left exactly as they are, with everything inside them
     */
    static Component rewrite(Component source, Pattern pattern, Function<String, Component> replacement,
                             Predicate<Component> skip) {
        if (skip.test(source)) {
            return source;
        }
        ComponentContents contents = source.getContents();
        MutableComponent result;
        if (contents instanceof PlainTextContents.LiteralContents literal && pattern.matcher(literal.text()).find()) {
            result = MutableComponent.create(PlainTextContents.EMPTY).setStyle(source.getStyle());
            splice(literal.text(), pattern, replacement, result);
        } else {
            result = MutableComponent.create(contents).setStyle(source.getStyle());
        }
        for (Component sibling : source.getSiblings()) {
            result.append(rewrite(sibling, pattern, replacement, skip));
        }
        return result;
    }

    /** The text in pieces: what lies between matches is plain text that takes the style around it. */
    private static void splice(String text, Pattern pattern, Function<String, Component> replacement,
                               MutableComponent into) {
        Matcher matcher = pattern.matcher(text);
        int copied = 0;
        while (matcher.find()) {
            if (matcher.start() > copied) {
                into.append(Component.literal(text.substring(copied, matcher.start())));
            }
            into.append(replacement.apply(matcher.group()));
            copied = matcher.end();
        }
        if (copied < text.length()) {
            into.append(Component.literal(text.substring(copied)));
        }
    }
}
