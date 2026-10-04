package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * A button of a server-described screen, drawn flat in the screen's theme. It comes in six looks:
 * a cell shows only its item, with the name and details in the tooltip, so many fit in a grid;
 * a tool is an action that shows only its item on a tinted surface, so it cannot be mistaken for an item
 * cell; a labeled button is a colored pill with text, for answers such as confirm and cancel; an action
 * shows its item and label; a glyph is a single character, used for navigation; a big one shows one
 * item large, such as the icon of a detail view. Left and right clicks are both reported, since the
 * server decides what each of them does.
 * <p>
 * Tooltips are drawn on one line per line of text, never wrapped, so an id or a long name stays whole.
 */
final class UiButtonWidget extends AbstractButton {

    static final int CELL_SIZE = 22;
    static final int TOOL_SIZE = 26;
    static final int ACTION_HEIGHT = 20;
    static final int GLYPH_SIZE = 18;
    static final int BIG_ICON = 48;

    private static final int BIG_PADDING = 8;
    private static final int ICON_SIZE = 16;
    private static final int PADDING = 5;
    private static final int MIN_ACTION_WIDTH = 40;
    private static final int MIN_LABELED_WIDTH = 70;
    private static final int LABELED_HEIGHT = 22;
    private static final int BADGE_COLOR = 0xFF7FE3A0;
    private static final int LABEL_COLOR = 0xFFFFFFFF;

    private enum Look {
        CELL, TOOL, LABELED, ACTION, GLYPH, BIG
    }

    /** What happens when the button is pressed. */
    @FunctionalInterface
    interface Press {
        void press(int mouseButton, boolean shift);
    }

    private final Font font;
    private final Look look;
    private final UiTheme.Tone tone;
    private final ItemStack icon;
    private final String glyph;
    private final String badge;
    private final boolean danger;
    private final Press press;
    private List<Component> tooltipLines = List.of();
    private int bigIcon = BIG_ICON;

    private UiButtonWidget(Font font, Look look, UiTheme.Tone tone, int width, int height, Component message,
                           ItemStack icon, String glyph, String badge, boolean danger, Press press) {
        super(0, 0, width, height, message);
        this.font = font;
        this.look = look;
        this.tone = tone;
        this.icon = icon;
        this.glyph = glyph;
        this.badge = badge;
        this.danger = danger;
        this.press = press;
    }

    /** A button that is only its item, with everything else in the tooltip. */
    static UiButtonWidget cell(Font font, UiElement.Button button, UiActions actions) {
        UiButtonWidget widget = new UiButtonWidget(font, Look.CELL, UiTheme.Tone.ACTION, CELL_SIZE, CELL_SIZE,
                shownLabel(button), button.icon(), null, button.badge(), false,
                (mouse, shift) -> actions.press(button.id(), mouse, shift));
        widget.describe(button, true);
        return widget;
    }

    /** An action: its item alone on a tinted surface, named by the tooltip. */
    static UiButtonWidget tool(Font font, UiElement.Button button, UiTheme.Tone tone, UiActions actions) {
        UiButtonWidget widget = new UiButtonWidget(font, Look.TOOL, tone, TOOL_SIZE, TOOL_SIZE, shownLabel(button),
                button.icon(), null, button.badge(), false, (mouse, shift) -> actions.press(button.id(), mouse, shift));
        widget.describe(button, true);
        return widget;
    }

    /** A colored pill with the button's label and no item, for answering a question. */
    static UiButtonWidget labeled(Font font, UiElement.Button button, UiTheme.Tone tone, UiActions actions) {
        // Plain text: the server colors labels for a chest, and a colored label would not read on a colored button.
        Component label = Component.literal(button.label().getString());
        int width = Math.max(MIN_LABELED_WIDTH, font.width(label) + 4 * PADDING);
        UiButtonWidget widget = new UiButtonWidget(font, Look.LABELED, tone, width, LABELED_HEIGHT, label, ItemStack.EMPTY,
                null, "", false, (mouse, shift) -> actions.press(button.id(), mouse, shift));
        widget.describe(button, false);
        return widget;
    }

    /** A small green check that saves an edit. */
    static UiButtonWidget check(Font font, Component hint, Press press) {
        UiButtonWidget widget = new UiButtonWidget(font, Look.LABELED, UiTheme.Tone.SUCCESS, TOOL_SIZE, LABELED_HEIGHT,
                Component.literal("✔"), ItemStack.EMPTY, null, "", false, press);
        widget.tooltipLines = List.of(hint);
        return widget;
    }

    /** A button with its item and label. */
    static UiButtonWidget action(Font font, UiElement.Button button, UiActions actions) {
        Component label = shownLabel(button);
        int iconSpace = button.icon().isEmpty() ? 0 : ICON_SIZE + PADDING;
        int width = Math.max(MIN_ACTION_WIDTH, PADDING + iconSpace + font.width(label) + PADDING);
        UiButtonWidget widget = new UiButtonWidget(font, Look.ACTION, UiTheme.Tone.ACTION, width, ACTION_HEIGHT, label,
                button.icon(), null, button.badge(), false, (mouse, shift) -> actions.press(button.id(), mouse, shift));
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
        UiButtonWidget widget = new UiButtonWidget(font, Look.GLYPH, UiTheme.Tone.ACTION, GLYPH_SIZE, GLYPH_SIZE,
                hint == null ? Component.literal(glyph) : hint, ItemStack.EMPTY, glyph, "", danger, press);
        widget.active = enabled;
        if (hint != null && enabled) {
            widget.tooltipLines = List.of(hint);
        }
        return widget;
    }

    /** One item drawn large, {@code size} pixels wide, that can be pressed, such as the icon of a detail view. */
    static UiButtonWidget big(Font font, ItemStack icon, int size, List<Component> tooltip, Press press) {
        UiButtonWidget widget = new UiButtonWidget(font, Look.BIG, UiTheme.Tone.ACTION, size + BIG_PADDING, size + BIG_PADDING,
                Component.empty(), icon, null, "", false, press);
        widget.bigIcon = size;
        widget.tooltipLines = List.copyOf(tooltip);
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
        tooltipLines = List.copyOf(lines);
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

    /** Vanilla's tooltip splits every line at a fixed width; this one keeps each line whole. */
    @Override
    protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (active && isHovered() && !tooltipLines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, tooltipLines, mouseX, mouseY);
        }
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
                drawBadge(graphics, getX() + getWidth() - 1, getY() + getHeight() - 1);
            }
            case TOOL -> {
                UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(), hovered ? tone.hoverFill : tone.fill,
                        hovered ? tone.hoverBorder : tone.border);
                // A bar along the bottom edge makes it read as a control, not as something that is held.
                graphics.fill(getX() + 3, getY() + getHeight() - 3, getX() + getWidth() - 3, getY() + getHeight() - 2,
                        hovered ? tone.hoverBorder : tone.border);
                int iconX = getX() + (getWidth() - ICON_SIZE) / 2;
                int iconY = getY() + (getHeight() - 3 - ICON_SIZE) / 2 + 1;
                graphics.item(icon, iconX, iconY);
                graphics.itemDecorations(font, icon, iconX, iconY);
            }
            case LABELED -> {
                UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(), hovered ? tone.hoverBorder : tone.border,
                        hovered ? tone.hoverBorder : tone.border);
                graphics.text(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
                        getY() + (getHeight() - font.lineHeight) / 2 + 1, LABEL_COLOR);
            }
            case ACTION -> {
                surface(graphics, hovered);
                int textX = getX() + PADDING;
                if (!icon.isEmpty()) {
                    int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
                    graphics.item(icon, textX, iconY);
                    graphics.itemDecorations(font, icon, textX, iconY);
                    drawBadge(graphics, textX + ICON_SIZE, getY() + (getHeight() + ICON_SIZE) / 2);
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
            case BIG -> {
                surface(graphics, hovered);
                graphics.pose().pushMatrix();
                graphics.pose().translate(getX() + (getWidth() - bigIcon) / 2.0F, getY() + (getHeight() - bigIcon) / 2.0F);
                graphics.pose().scale(bigIcon / (float) ICON_SIZE, bigIcon / (float) ICON_SIZE);
                graphics.item(icon, 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }

    /** The mark of a button that has one, such as a check, in the lower right corner of its item. */
    private void drawBadge(GuiGraphicsExtractor graphics, int right, int bottom) {
        if (!badge.isEmpty()) {
            graphics.text(font, badge, right - font.width(badge), bottom - font.lineHeight, BADGE_COLOR);
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
