package com.panita.enriquecraft.core.dialog;

import net.minecraft.network.chat.Component;

/**
 * A piece of the body of a dialog, which the game draws above the inputs. Everything is centered and in one column, since the game lays a
 * dialog out that way.
 */
public sealed interface DialogBlock {

    /** Text that wraps to the width of the dialog; a line break in it starts a new line. */
    record Message(Component text) implements DialogBlock {
    }
}
