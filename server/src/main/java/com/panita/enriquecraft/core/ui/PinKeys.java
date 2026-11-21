package com.panita.enriquecraft.core.ui;

/**
 * What the keys of a PIN keypad do. The screen reports a key and nothing else: the digits typed live on
 * the server, with whoever asked for them.
 */
public interface PinKeys {

    void digit(int digit);

    void backspace();

    void clear();
}
