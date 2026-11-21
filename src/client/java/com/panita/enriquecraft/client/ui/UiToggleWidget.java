package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * A checkbox of a server-described screen: a small box that holds a check, with its label beside it. Pressing it
 * flips the box at once and tells the server the state it now has; the server answers with the screen again, which
 * brings the state it settled on.
 */
final class UiToggleWidget extends AbstractButton {

    private static final int BOX_SIZE = 12;
    private static final int GAP = 6;
    private static final int HEIGHT = 18;
    private static final int CHECK_INSET = 3;

    private final Font font;
    private final int id;
    private final UiActions actions;
    private boolean checked;

    UiToggleWidget(Font font, UiElement.Toggle toggle, UiActions actions) {
        super(0, 0, BOX_SIZE + GAP + font.width(toggle.label()), HEIGHT, toggle.label());
        this.font = font;
        this.id = toggle.id();
        this.actions = actions;
        this.checked = toggle.checked();
    }

    @Override
    public void onPress(InputWithModifiers input) {
        checked = !checked;
        actions.select(id, checked ? 1 : 0);
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return button.button() == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        int boxX = getX();
        int boxY = getY() + (getHeight() - BOX_SIZE) / 2;
        UiTheme.pill(graphics, boxX, boxY, BOX_SIZE, BOX_SIZE, checked ? UiTheme.Tone.SUCCESS.fill() : UiTheme.CELL_EMPTY,
                checked ? UiTheme.Tone.SUCCESS.border() : hovered ? UiTheme.ACCENT : UiTheme.CELL_BORDER);
        if (checked) {
            // A check drawn from two strokes, so it needs no glyph the font might lack.
            int left = boxX + CHECK_INSET;
            int top = boxY + CHECK_INSET;
            int color = UiTheme.Tone.SUCCESS.hoverBorder();
            for (int step = 0; step < 3; step++) {
                graphics.fill(left + step, top + 3 + step, left + step + 1, top + 4 + step, color);
            }
            for (int step = 1; step < 5; step++) {
                graphics.fill(left + 2 + step, top + 5 - step, left + 3 + step, top + 6 - step, color);
            }
        }
        graphics.text(font, getMessage(), boxX + BOX_SIZE + GAP, getY() + (getHeight() - font.lineHeight) / 2 + 1,
                hovered ? UiTheme.TEXT : UiTheme.MUTED);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
