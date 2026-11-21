package com.panita.enriquecraft.core.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks a text into lines of at most a number of characters, for places that do not wrap by themselves, such as a tooltip.
 */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * Lines of at most {@code width} characters, broken at spaces. The line breaks the text already has are kept, and a word longer
     * than a line is cut, so no line is ever longer than asked.
     */
    public static List<String> lines(String text, int width) {
        if (width < 1) {
            throw new IllegalArgumentException("A line holds at least one character");
        }
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            wrap(paragraph, width, lines);
        }
        return lines;
    }

    private static void wrap(String paragraph, int width, List<String> lines) {
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.strip().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String rest = word;
            while (rest.length() > width) {
                flush(line, lines);
                lines.add(rest.substring(0, width));
                rest = rest.substring(width);
            }
            if (!line.isEmpty() && line.length() + 1 + rest.length() > width) {
                flush(line, lines);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(rest);
        }
        lines.add(line.toString());
    }

    private static void flush(StringBuilder line, List<String> lines) {
        if (!line.isEmpty()) {
            lines.add(line.toString());
            line.setLength(0);
        }
    }
}
