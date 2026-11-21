package com.panita.enriquecraft.core.message;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Breaks styled text into lines of a width, at spaces, keeping the style of every part: the counterpart of {@link TextWrap} for text
 * that already carries colors, such as what LuckPerms gives for a meta value. A line break in the text is kept.
 */
public final class StyledWrap {

    private StyledWrap() {
    }

    /** The text with a line break wherever a line would pass {@code width} characters; a word longer than that stays on one line. */
    public static Component wrap(Component text, int width) {
        MutableComponent out = Component.empty();
        int column = 0;
        // A space is only written once the word after it is known to stay on the line, so no line ends in one
        boolean spacePending = false;
        Style spaceStyle = Style.EMPTY;
        for (Component part : text.toFlatList()) {
            String[] lines = part.getString().split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                if (index > 0) {
                    out.append(Component.literal("\n"));
                    column = 0;
                    spacePending = false;
                }
                for (String token : lines[index].split("(?<= )|(?= )")) {
                    if (token.isEmpty()) {
                        continue;
                    }
                    if (token.equals(" ")) {
                        spacePending = column > 0;
                        spaceStyle = part.getStyle();
                        continue;
                    }
                    int needed = token.length() + (spacePending ? 1 : 0);
                    if (column > 0 && column + needed > width) {
                        out.append(Component.literal("\n"));
                        column = 0;
                        spacePending = false;
                    }
                    if (spacePending) {
                        out.append(Component.literal(" ").withStyle(spaceStyle));
                        column++;
                        spacePending = false;
                    }
                    out.append(Component.literal(token).withStyle(part.getStyle()));
                    column += token.length();
                }
            }
        }
        return out;
    }
}
