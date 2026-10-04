package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * A button of a server-described screen, drawn flat in the screen's theme. It comes in three looks:
 * a cell shows only its item, with the name and details in the tooltip, so many fit in a grid; an
 * action shows its item and label; a glyph is a single character, used for navigation. Left and
 * right clicks are both reported, since the server decides what each of them does.
 */
final class UiButtonWidget extends AbstractButton {

    static final int CELL_SIZE = 22;
    static final int ACTION_HEIGHT = 20;
    static final int GLYPH_SIZE = 18;

    private static final int ICON_SIZE = 16;
    private static final int PADDING = 5;
    private static final int MIN_ACTION_WIDTH = 40;

    private enum Look {
        CELL, ACTION, GLYPH
    }

    /** What happens when the button is pressed. */
    @FunctionalInterface
    interface Press {
        void press(int mouseButton, boolean shift);
    }

    private final Font font;
    private final Look look;
    private final ItemStack icon;
    private final String glyph;
    private final boolean danger;
    private final Press press;

    private UiButtonWidget(Font font, Look look, int width, int height, Component message, ItemStack icon, String glyph,
                           boolean danger, Press press) {
        super(0, 0, width, height, message);
        this.font = font;
        this.look = look;
        this.icon = icon;
        this.glyph = glyph;
        this.danger = danger;
        this.press = press;
    }

    /** A button of a grid: only its item, with everything else in the tooltip. */
    static UiButtonWidget cell(Font font, UiElement.Button button, UiActions actions) {
        UiButtonWidget widget = new UiButtonWidget(font, Look.CELL, CELL_SIZE, CELL_SIZE, shownLabel(button), button.icon(),
                null, false, (mouse, shift) -> actions.press(button.id(), mouse, shift));
        widget.describe(button, true);
        return widget;
    }

    /** A button with its item and label. */
    static UiButtonWidget action(Font font, UiElement.Button button, UiActions actions) {
        Component label = shownLabel(button);
        int iconSpace = button.icon().isEmpty() ? 0 : ICON_SIZE + PADDING;
        int width = Math.max(MIN_ACTION_WIDTH, PADDING + iconSpace + font.width(label) + PADDING);
        UiButtonWidget widget = new UiButtonWidget(font, Look.ACTION, width, ACTION_HEIGHT, label, button.icon(), null, false,
                (mouse, shift) -> actions.press(button.id(), mouse, shift));
        widget.describe(button, false);
        return widget;
    }

    /**
     * A single character on a small button, for navigation.
     *
     * @param hint    shown as a tooltip, or null for none
     * @param danger  whether hovering it turns it red, for buttons that discard something
     * @param enabled disabled glyphs are dim and do nothing
     */
    static UiButtonWidget glyph(Font font, String glyph, Component hint, boolean danger, boolean enabled, Press press) {
        UiButtonWidget widget = new UiButtonWidget(font, Look.GLYPH, GLYPH_SIZE, GLYPH_SIZE,
                hint == null ? Component.literal(glyph) : hint, ItemStack.EMPTY, glyph, danger, press);
        widget.active = enabled;
        if (hint != null && enabled) {
            widget.setTooltip(Tooltip.create(hint));
        }
        return widget;
    }

    private void describe(UiElement.Button button, boolean compact) {
        List<Component> lines = new ArrayList<>();
        if (isItemOnly(button)) {
            lines.addAll(itemTooltip(button.icon()));
        } else if (compact && !button.label().getString().isEmpty()) {
            lines.add(button.label());
        }
        lines.addAll(button.tooltip());
        if (!lines.isEmpty()) {
            setTooltip(Tooltip.create(joined(lines)));
        }
    }

    /** A button that is only an item, such as a stack in an inventory view, is named and described by the item. */
    private static boolean isItemOnly(UiElement.Button button) {
        return button.label().getString().isEmpty() && !button.icon().isEmpty();
    }

    private static Component shownLabel(UiElement.Button button) {
        return isItemOnly(button) ? button.icon().getHoverName() : button.label();
    }

    private static List<Component> itemTooltip(ItemStack icon) {
        Minecraft minecraft = Minecraft.getInstance();
        return icon.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
    }

    private static Component joined(List<Component> lines) {
        MutableComponent text = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                text.append(Component.literal("\n"));
            }
            text.append(lines.get(index));
        }
        return text;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        // The protocol numbers the buttons 0 and 1; Minecraft numbers them its own way.
        boolean right = input instanceof MouseButtonEvent event && event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        press.press(right ? 1 : 0, input.hasShiftDown());
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return button.button() == InputConstants.MOUSE_BUTTON_LEFT || button.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isHoveredOrFocused();
        switch (look) {
            case CELL -> {
                surface(graphics, hovered);
                int iconX = getX() + (getWidth() - ICON_SIZE) / 2;
                int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
                graphics.item(icon, iconX, iconY);
                graphics.itemDecorations(font, icon, iconX, iconY);
            }
            case ACTION -> {
                surface(graphics, hovered);
                int textX = getX() + PADDING;
                if (!icon.isEmpty()) {
                    int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
                    graphics.item(icon, textX, iconY);
                    graphics.itemDecorations(font, icon, textX, iconY);
                    textX += ICON_SIZE + PADDING;
                }
                graphics.text(font, getMessage(), textX, getY() + (getHeight() - font.lineHeight) / 2, UiTheme.TEXT);
            }
            case GLYPH -> {
                if (hovered) {
                    UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(),
                            danger ? UiTheme.DANGER : UiTheme.CELL_HOVER, danger ? UiTheme.DANGER : UiTheme.CELL_HOVER);
                }
                int color = !active ? UiTheme.DISABLED : hovered ? UiTheme.TEXT : UiTheme.MUTED;
                graphics.text(font, glyph, getX() + (getWidth() - font.width(glyph)) / 2,
                        getY() + (getHeight() - font.lineHeight) / 2 + 1, color);
            }
        }
    }

    private void surface(GuiGraphicsExtractor graphics, boolean hovered) {
        UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(),
                hovered ? UiTheme.CELL_HOVER : UiTheme.CELL, hovered ? UiTheme.ACCENT : UiTheme.CELL_BORDER);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
