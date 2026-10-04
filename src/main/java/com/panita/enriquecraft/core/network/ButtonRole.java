package com.panita.enriquecraft.core.network;

/**
 * What a button is for, when that is more than its label says. The client companion draws the
 * standard navigation buttons in the same place on every screen and gives each purpose its own look,
 * so an action never resembles an item; a chest ignores the role.
 * <p>
 * Values are sent by position: add new ones at the end.
 */
public enum ButtonRole {

    /** An ordinary action, drawn where the screen puts it. */
    NONE,

    /** Closes the screen. */
    CLOSE,

    /** Goes back to the screen this one was opened from. */
    BACK,

    /** Shows the previous page of a list. */
    PREVIOUS,

    /** Confirms what the screen asks about; the client companion shows it as a green labeled button. */
    CONFIRM,

    /** Shows the next page of a list. */
    NEXT,

    /**
     * Turns down what the screen asks about and goes back; the client companion shows it as a red labeled
     * button, and Escape presses it when there is no {@link #BACK}.
     */
    CANCEL,

    /** An action that discards something, such as a delete; red in the client companion. */
    DANGER,

    /** An action that gives something back or completes something; green in the client companion. */
    SUCCESS
}
