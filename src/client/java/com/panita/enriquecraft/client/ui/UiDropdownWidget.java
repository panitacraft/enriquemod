package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A dropdown of a server-described screen: a button that shows the current choice and opens a list of
 * the options. The list is not part of the widget tree, because it must sit above everything and take
 * the clicks that land on it first; the screen asks the open dropdown to draw it and to handle clicks.
 */
final class UiDropdownWidget extends AbstractButton {

    private static final int HEIGHT = UiButtonWidget.GLYPH_SIZE + 2;
    private static final int PADDING = 6;
    private static final int ROW_HEIGHT = 16;
    private static final int ARROW_SPACE = 12;

    private final Font font;
    private final UiElement.Dropdown dropdown;
    private final UiActions actions;
    private boolean open;

    UiDropdownWidget(Font font, UiElement.Dropdown dropdown, UiActions actions) {
        super(0, 0, widthOf(font, dropdown), HEIGHT, dropdown.label());
        this.font = font;
        this.dropdown = dropdown;
        this.actions = actions;
    }

    /** Wide enough for the label and for every option, so the button never changes size when the choice does. */
    private static int widthOf(Font font, UiElement.Dropdown dropdown) {
        int widest = font.width(dropdown.label());
        for (var option : dropdown.options()) {
            widest = Math.max(widest, font.width(option));
        }
        return PADDING + widest + ARROW_SPACE + PADDING;
    }

    boolean isOpen() {
        return open;
    }

    void close() {
        open = false;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        open = !open;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused() || open;
        UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(),
                hovered ? UiTheme.CELL_HOVER : UiTheme.CELL_EMPTY, open ? UiTheme.ACCENT : UiTheme.CELL_BORDER);
        // With no choice made it says what it is for; otherwise it says what is chosen.
        var shown = dropdown.selected() == 0 ? dropdown.label() : dropdown.options().get(dropdown.selected());
        graphics.text(font, shown, getX() + PADDING, getY() + (getHeight() - font.lineHeight) / 2, UiTheme.TEXT);
        graphics.text(font, open ? "▴" : "▾", getX() + getWidth() - PADDING - font.width("▾"),
                getY() + (getHeight() - font.lineHeight) / 2, UiTheme.MUTED);
    }

    private int listHeight() {
        return dropdown.options().size() * ROW_HEIGHT + 4;
    }

    private int listTop() {
        return getY() - listHeight() - 2;
    }

    /** Draws the list of options above the button. Call it after everything else has been drawn. */
    void extractList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        int top = listTop();
        UiTheme.pill(graphics, getX(), top, getWidth(), listHeight(), UiTheme.PANEL | 0xFF000000, UiTheme.ACCENT);
        for (int option = 0; option < dropdown.options().size(); option++) {
            int rowTop = top + 2 + option * ROW_HEIGHT;
            boolean hovered = mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(getX() + 2, rowTop, getX() + getWidth() - 2, rowTop + ROW_HEIGHT, UiTheme.CELL_HOVER);
            }
            if (option == dropdown.selected()) {
                graphics.text(font, "▸", getX() + 3, rowTop + (ROW_HEIGHT - font.lineHeight) / 2, UiTheme.ACCENT);
            }
            graphics.text(font, dropdown.options().get(option), getX() + PADDING + 4,
                    rowTop + (ROW_HEIGHT - font.lineHeight) / 2, UiTheme.TEXT);
        }
    }

    /**
     * Handles a click while the list is open: choosing an option reports it, and any click closes the list.
     *
     * @return whether the click landed on the list, so nothing underneath should react to it
     */
    boolean clickList(MouseButtonEvent event) {
        if (!open) {
            return false;
        }
        boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        double x = event.x();
        double y = event.y();
        int top = listTop();
        boolean inside = x >= getX() && x < getX() + getWidth() && y >= top + 2 && y < top + 2 + dropdown.options().size() * ROW_HEIGHT;
        if (inside && left) {
            actions.select(dropdown.id(), (int) ((y - top - 2) / ROW_HEIGHT));
        }
        // A click on the button itself is left to the button, which toggles the list.
        if (!(x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight())) {
            open = false;
        }
        return inside;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
