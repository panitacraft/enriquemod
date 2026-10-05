package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * The look of the client companion's screens: flat dark surfaces with soft corners and one accent
 * color, the amber of the mod's chat prefix.
 */
final class UiTheme {

    static final int PANEL = 0xF2121212;
    static final int PANEL_BORDER = 0xFF3A3A3A;
    static final int DIVIDER = 0xFF2E2E2E;
    static final int ACCENT = 0xFFF2B134;

    static final int CELL = 0xFF222222;
    static final int CELL_HOVER = 0xFF383838;
    static final int CELL_EMPTY = 0xFF181818;
    static final int CELL_BORDER = 0xFF303030;

    static final int DANGER = 0xFFB3362F;

    /** What an action looks like, so it never resembles an item cell: a tinted surface per purpose. */
    record Tone(int fill, int hoverFill, int border, int hoverBorder) {
        static final Tone ACTION = new Tone(0xFF22344A, 0xFF2C4560, 0xFF4A7BB5, 0xFF6FA3E0);
        static final Tone SUCCESS = new Tone(0xFF1F4D33, 0xFF286642, 0xFF3FA66B, 0xFF5FCB8D);
        static final Tone DANGER = new Tone(0xFF5A2320, 0xFF7A2E2A, 0xFFB3362F, 0xFFE05A50);

        /** A tone built around one color the server chose: it is the border, and the surface is a dark shade of it. */
        static Tone of(int rgb) {
            int border = 0xFF000000 | rgb;
            return new Tone(mix(border, 0xFF101010, 0.55F), mix(border, 0xFF101010, 0.4F), border,
                    mix(border, 0xFFFFFFFF, 0.25F));
        }
    }

    /** Each color channel part of the way from {@code from} to {@code to}. */
    private static int mix(int from, int to, float amount) {
        int result = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (from >> shift) & 0xFF;
            int b = (to >> shift) & 0xFF;
            result |= Math.round(a + (b - a) * amount) << shift;
        }
        return result;
    }

    /** The font of titles; defined in {@code assets/enriquecraft/font/title.json}. */
    static final FontDescription TITLE_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("enriquecraft", "title"));

    static final int TEXT = 0xFFF0F0F0;
    static final int MUTED = 0xFF8E8E8E;
    static final int DISABLED = 0xFF4A4A4A;

    private UiTheme() {
    }

    /** A rectangle with a one pixel border and its corner pixels cut, which reads as rounded. */
    static void pill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x + 1, y, x + width - 1, y + height, border);
        graphics.fill(x, y + 1, x + width, y + height - 1, border);
        graphics.fill(x + 2, y + 1, x + width - 2, y + height - 1, fill);
        graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, fill);
    }
}
