package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;

/**
 * What a PIN keypad remembers between the descriptions the server sends: a screen is rebuilt on every
 * update, so the moments the animations started at live here, in the screen, not in the widget. The widget
 * tells it about every description and asks it how far each animation has come.
 */
final class PinPadAnimation {

    static final long POP_MILLIS = 140;
    static final long SHAKE_MILLIS = 420;
    static final long PRESS_MILLIS = 130;
    static final long SUCCESS_MILLIS = 600;

    private static final long LONG_AGO = Long.MIN_VALUE / 2;

    private int filled;
    private int shakes;
    private boolean success;
    private long popAt = LONG_AGO;
    private long shakeAt = LONG_AGO;
    private long successAt = LONG_AGO;
    private int pressedKey = -1;
    private long pressedAt = LONG_AGO;

    /** Notes a new description of the keypad and starts the animations it calls for. */
    void observe(UiElement.PinPad pad, long now) {
        if (pad.shakes() > shakes) {
            shakeAt = now;
        }
        if (pad.filled() > filled) {
            popAt = now;
        }
        if (pad.success() && !success) {
            successAt = now;
        }
        shakes = pad.shakes();
        filled = pad.filled();
        success = pad.success();
    }

    void press(int key, long now) {
        pressedKey = key;
        pressedAt = now;
    }

    /** The key that was just pressed and still shows it, or -1. */
    int pressedKey(long now) {
        return now - pressedAt < PRESS_MILLIS ? pressedKey : -1;
    }

    /** Whether the dots are still showing that the PIN was wrong. */
    boolean isShaking(long now) {
        return now - shakeAt < SHAKE_MILLIS;
    }

    /** How far the dots are pushed sideways, in pixels: a shake that fades out. */
    int shakeOffset(long now) {
        long elapsed = now - shakeAt;
        if (elapsed >= SHAKE_MILLIS) {
            return 0;
        }
        double fade = 1.0 - elapsed / (double) SHAKE_MILLIS;
        return (int) Math.round(Math.sin(elapsed / 28.0) * 6.0 * fade);
    }

    /** How large the dot that was filled last is drawn, from 1: it grows a little and settles. */
    float popScale(long now) {
        long elapsed = now - popAt;
        if (elapsed >= POP_MILLIS) {
            return 1.0F;
        }
        return 1.0F + 0.35F * (float) Math.sin(Math.PI * elapsed / POP_MILLIS);
    }

    /** How far the celebration has come, from 0 to 1; it is 1 for good once it is over. */
    float successProgress(long now) {
        return Math.min(1.0F, (now - successAt) / (float) SUCCESS_MILLIS);
    }
}
