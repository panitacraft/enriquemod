package com.panita.enriquecraft.core.ui;

/**
 * How a screen is decorated when it is shown as a vanilla chest. The client companion's screen
 * ignores it.
 */
public enum ChestStyle {

    /** Elements only; empty slots stay empty. */
    PLAIN,

    /**
     * The outer edge is filler. Every band of the screen but the last goes inside the frame, and the
     * last takes the bottom edge, where controls belong.
     */
    FRAMED,

    /** Every slot without an element is filler. */
    FILLED,

    /**
     * Every slot without an element is filler, except the cells of a grid, which stay empty: a cell of a grid
     * stands for a slot of something, such as an inventory, and an empty one says that slot held nothing.
     */
    SLOTS
}
