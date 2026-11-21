package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * A line of text as the bot draws it: a few runs of text, each with the color, font and marks the game gave it. The bot never
 * receives game text components, only this plain shape, so it has nothing to parse and nothing a component could smuggle in. A
 * run is {@code {t, c?, f?, b?, i?, u?, s?, o?}}: the text, a {@code #rrggbb} color, a font id such as {@code minecraft:default}
 * left out when it is the default, and a flag for each mark that is on. The line is cut to what a frame can hold.
 */
public final class DisplayRuns {

    /** The most runs and characters of a line, which keeps a list of players well inside a frame. */
    public static final int MAX_RUNS = 16;
    public static final int MAX_CHARACTERS = 80;
    private static final Identifier DEFAULT_FONT = Identifier.withDefaultNamespace("default");

    private DisplayRuns() {
    }

    public static JsonArray of(Component line) {
        JsonArray runs = new JsonArray();
        int[] room = {MAX_CHARACTERS};
        line.visit((style, text) -> {
            if (text.isEmpty() || room[0] <= 0 || runs.size() >= MAX_RUNS) {
                return Optional.empty();
            }
            String cut = text.length() <= room[0] ? text : text.substring(0, room[0]);
            room[0] -= cut.length();
            runs.add(run(style, cut));
            return Optional.empty();
        }, Style.EMPTY);
        return runs;
    }

    private static JsonObject run(Style style, String text) {
        JsonObject run = new JsonObject();
        run.addProperty("t", text);
        TextColor color = style.getColor();
        if (color != null) {
            run.addProperty("c", String.format("#%06x", color.getValue() & 0xFFFFFF));
        }
        if (style.getFont() instanceof FontDescription.Resource resource && !DEFAULT_FONT.equals(resource.id())) {
            run.addProperty("f", resource.id().toString());
        }
        mark(run, "b", style.isBold());
        mark(run, "i", style.isItalic());
        mark(run, "u", style.isUnderlined());
        mark(run, "s", style.isStrikethrough());
        mark(run, "o", style.isObfuscated());
        return run;
    }

    private static void mark(JsonObject run, String key, boolean on) {
        if (on) {
            run.addProperty(key, true);
        }
    }
}
