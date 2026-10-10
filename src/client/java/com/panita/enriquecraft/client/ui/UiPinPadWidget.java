package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * The PIN keypad: a headline, a row of dots that fill as digits are typed, the attempts that are left as
 * small pips, and twelve keys. It draws what the server described and reports presses of its keys; the
 * digits themselves are only ever known to the server. Wrong PINs shake the dots, a new digit pops, a
 * correct PIN turns them green.
 */
final class UiPinPadWidget extends AbstractWidget {

    private static final int KEY_WIDTH = 38;
    private static final int KEY_HEIGHT = 26;
    private static final int KEY_GAP = 5;
    private static final int KEY_COLUMNS = 3;
    private static final int KEY_ROWS = 4;
    private static final int DOT = 10;
    private static final int DOT_GAP = 7;
    private static final int PIP = 6;
    private static final int PIP_GAP = 5;
    private static final int GAP_AFTER_TITLE = 4;
    private static final int GAP_AFTER_HINT = 12;
    private static final int GAP_AFTER_DOTS = 7;
    private static final int GAP_AFTER_PIPS = 12;
    private static final float TITLE_SCALE = 1.25F;
    private static final int SUCCESS_COLOR = 0xFF3FA66B;
    private static final int USED_PIP = 0xFF5A2320;

    private final Font font;
    private final UiElement.PinPad pad;
    private final UiActions actions;
    private final PinPadAnimation animation;
    private final Component title;
    private final int keypadWidth = KEY_COLUMNS * KEY_WIDTH + (KEY_COLUMNS - 1) * KEY_GAP;
    private final int titleHeight;
    private final int dotsTop;
    private final int pipsTop;
    private final int keypadTop;

    UiPinPadWidget(Font font, UiElement.PinPad pad, UiActions actions) {
        super(0, 0, 0, 0, pad.title());
        this.font = font;
        this.pad = pad;
        this.actions = actions;
        this.animation = actions.pinPadAnimation();
        this.title = pad.title().copy().withStyle(style -> style.withFont(UiTheme.TITLE_FONT));
        this.titleHeight = Math.round(font.lineHeight * TITLE_SCALE);
        this.dotsTop = titleHeight + GAP_AFTER_TITLE + font.lineHeight + GAP_AFTER_HINT;
        this.pipsTop = dotsTop + DOT + GAP_AFTER_DOTS;
        this.keypadTop = pipsTop + PIP + GAP_AFTER_PIPS;
        int dotsWidth = pad.length() * DOT + (pad.length() - 1) * DOT_GAP;
        setWidth(Math.max(keypadWidth, Math.max(dotsWidth, Math.max(
                Math.round(font.width(title) * TITLE_SCALE), font.width(pad.hint())))));
        setHeight(keypadTop + KEY_ROWS * KEY_HEIGHT + (KEY_ROWS - 1) * KEY_GAP);
        animation.observe(pad, System.currentTimeMillis());
    }

    /** Which of the twelve keys sits at a place on the keypad: the digits, then backspace, zero and clear. */
    private static int keyAt(int column, int row) {
        int place = row * KEY_COLUMNS + column;
        return switch (place) {
            case 9 -> UiElement.PinPad.BACKSPACE;
            case 10 -> 0;
            case 11 -> UiElement.PinPad.CLEAR;
            default -> place + 1;
        };
    }

    private int keyLeft(int column) {
        return getX() + (getWidth() - keypadWidth) / 2 + column * (KEY_WIDTH + KEY_GAP);
    }

    private int keyTop(int row) {
        return getY() + keypadTop + row * (KEY_HEIGHT + KEY_GAP);
    }

    /** The key under a point, or -1 when it is between keys or outside the keypad. */
    private int keyUnder(double x, double y) {
        for (int row = 0; row < KEY_ROWS; row++) {
            for (int column = 0; column < KEY_COLUMNS; column++) {
                int left = keyLeft(column);
                int top = keyTop(row);
                if (x >= left && x < left + KEY_WIDTH && y >= top && y < top + KEY_HEIGHT) {
                    return keyAt(column, row);
                }
            }
        }
        return -1;
    }

    /** Presses a key as a click or the keyboard does; nothing happens once the PIN was accepted. */
    void pressKey(int key) {
        if (pad.success()) {
            return;
        }
        animation.press(key, System.currentTimeMillis());
        actions.press(pad.firstKeyId() + key, 0, false);
    }

    /** The key a physical key stands for: digits and numpad digits, Backspace, Delete; or -1. */
    static int keyOfKeyboard(int glfwKey) {
        if (glfwKey >= InputConstants.KEY_0 && glfwKey <= InputConstants.KEY_9) {
            return glfwKey - InputConstants.KEY_0;
        }
        if (glfwKey >= InputConstants.KEY_NUMPAD0 && glfwKey <= InputConstants.KEY_NUMPAD9) {
            return glfwKey - InputConstants.KEY_NUMPAD0;
        }
        if (glfwKey == InputConstants.KEY_BACKSPACE) {
            return UiElement.PinPad.BACKSPACE;
        }
        if (glfwKey == InputConstants.KEY_DELETE) {
            return UiElement.PinPad.CLEAR;
        }
        return -1;
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return button.button() == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return keyUnder(mouseX, mouseY) >= 0;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int key = keyUnder(event.x(), event.y());
        if (key >= 0) {
            pressKey(key);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        int centerX = getX() + getWidth() / 2;
        extractTexts(graphics, centerX);
        extractDots(graphics, now, centerX);
        extractPips(graphics, centerX);
        extractKeys(graphics, now, mouseX, mouseY);
    }

    private void extractTexts(GuiGraphicsExtractor graphics, int centerX) {
        int titleWidth = Math.round(font.width(title) * TITLE_SCALE);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - titleWidth / 2.0F, getY());
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
        graphics.text(font, title, 0, 0, UiTheme.TEXT);
        graphics.pose().popMatrix();
        graphics.centeredText(font, pad.hint(), centerX, getY() + titleHeight + GAP_AFTER_TITLE, UiTheme.MUTED);
    }

    private void extractDots(GuiGraphicsExtractor graphics, long now, int centerX) {
        int count = pad.length();
        int width = count * DOT + (count - 1) * DOT_GAP;
        int left = centerX - width / 2 + animation.shakeOffset(now);
        boolean wrong = animation.isShaking(now);
        float celebration = pad.success() ? animation.successProgress(now) : 0.0F;
        for (int index = 0; index < count; index++) {
            int x = left + index * (DOT + DOT_GAP);
            int y = getY() + dotsTop;
            boolean filled = index < pad.filled();
            if (!filled) {
                UiTheme.pill(graphics, x, y, DOT, DOT, UiTheme.CELL_EMPTY, UiTheme.CELL_BORDER);
                continue;
            }
            int color = pad.success() ? SUCCESS_COLOR : wrong ? UiTheme.DANGER : UiTheme.ACCENT;
            // The dot filled last pops; on success the whole row swells once, one dot after the other.
            float scale = index == pad.filled() - 1 && !pad.success() ? animation.popScale(now) : 1.0F;
            if (pad.success()) {
                float wave = Math.max(0.0F, 1.0F - Math.abs(celebration * (count + 2) - index - 1) / 2.0F);
                scale = 1.0F + 0.4F * wave;
            }
            int size = Math.round(DOT * scale);
            int offset = (size - DOT) / 2;
            UiTheme.pill(graphics, x - offset, y - offset, size, size, color, color);
        }
    }

    private void extractPips(GuiGraphicsExtractor graphics, int centerX) {
        int count = pad.maxAttempts();
        if (count == 0) {
            return;
        }
        int width = count * PIP + (count - 1) * PIP_GAP;
        int left = centerX - width / 2;
        for (int index = 0; index < count; index++) {
            int color = index < pad.attemptsLeft() ? SUCCESS_COLOR : USED_PIP;
            UiTheme.pill(graphics, left + index * (PIP + PIP_GAP), getY() + pipsTop, PIP, PIP, color, color);
        }
    }

    private void extractKeys(GuiGraphicsExtractor graphics, long now, int mouseX, int mouseY) {
        int pressed = animation.pressedKey(now);
        int hovered = pad.success() ? -1 : keyUnder(mouseX, mouseY);
        for (int row = 0; row < KEY_ROWS; row++) {
            for (int column = 0; column < KEY_COLUMNS; column++) {
                int key = keyAt(column, row);
                UiTheme.Tone tone = key == UiElement.PinPad.CLEAR ? UiTheme.Tone.DANGER : UiTheme.Tone.ACTION;
                boolean hover = key == hovered;
                int fill = key == pressed ? UiTheme.Tone.SUCCESS.hoverFill() : hover ? tone.hoverFill() : tone.fill();
                int border = key == pressed ? UiTheme.Tone.SUCCESS.hoverBorder() : hover ? tone.hoverBorder() : tone.border();
                int left = keyLeft(column);
                int top = keyTop(row);
                UiTheme.pill(graphics, left, top, KEY_WIDTH, KEY_HEIGHT, fill, border);
                graphics.centeredText(font, label(key), left + KEY_WIDTH / 2, top + (KEY_HEIGHT - font.lineHeight) / 2 + 1,
                        pad.success() ? UiTheme.DISABLED : UiTheme.TEXT);
            }
        }
    }

    private static String label(int key) {
        return switch (key) {
            case UiElement.PinPad.BACKSPACE -> "←";
            case UiElement.PinPad.CLEAR -> "✕";
            default -> String.valueOf(key);
        };
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
