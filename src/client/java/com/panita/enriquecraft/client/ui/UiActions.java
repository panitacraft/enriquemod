package com.panita.enriquecraft.client.ui;

/**
 * What a player can do on a server-described screen, reported to the server.
 */
interface UiActions {

    /**
     * @param elementId the id the server gave the button
     * @param button    the mouse button: 0 is left, 1 is right
     */
    void press(int elementId, int button, boolean shift);

    /**
     * @param elementId the id the server gave the text field
     * @param text      the value the player confirmed
     */
    void submit(int elementId, String text);

    /**
     * @param elementId the id the server gave the dropdown
     * @param option    the position of the option the player chose
     */
    void select(int elementId, int option);

    /**
     * @param draggedId the id of the button the player dragged
     * @param targetId  the id of the button they dropped it on
     */
    void drop(int draggedId, int targetId);
}
