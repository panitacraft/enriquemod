package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

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
