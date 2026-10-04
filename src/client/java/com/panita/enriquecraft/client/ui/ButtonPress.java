package com.panita.enriquecraft.client.ui;

/**
 * Reports a press on a button of a server-described screen.
 */
@FunctionalInterface
interface ButtonPress {

    /**
     * @param elementId the id the server gave the button
     * @param button    the mouse button: 0 is left, 1 is right
     */
    void press(int elementId, int button, boolean shift);
}
