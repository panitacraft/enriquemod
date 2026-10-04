package com.panita.enriquecraft.core.network;

/**
 * What a button is for, when that is more than its label says. The client companion draws the
 * standard navigation buttons in the same place on every screen; a chest ignores the role.
 */
public enum ButtonRole {

    /** An ordinary button, drawn where the screen puts it. */
    NONE,

    /** Closes the screen. */
    CLOSE,

    /** Goes back to the screen this one was opened from. */
    BACK,

    /** Shows the previous page of a list. */
    PREVIOUS,

    /** Confirms what the screen asks about; the client companion shows it as a labeled button. */
    CONFIRM,

    /** Shows the next page of a list. */
    NEXT
}
