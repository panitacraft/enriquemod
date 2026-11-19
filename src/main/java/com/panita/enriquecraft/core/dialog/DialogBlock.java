package com.panita.enriquecraft.core.dialog;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A piece of the body of a dialog, which the game draws above the inputs: some text, or an item drawn large with text beside it.
 * Everything is centered and in one column, since the game lays a dialog out that way.
 */
public sealed interface DialogBlock {

    /** The sizes an item may be drawn at, in pixels; the game accepts up to 256. */
    int MIN_ITEM_SIZE = 16;
    int MAX_ITEM_SIZE = 128;

    /** Text that wraps to the width of the dialog; a line break in it starts a new line. */
    record Message(Component text) implements DialogBlock {
    }

    /**
     * An item with text beside it, such as the head of a player and who they are. The item is only drawn: no tooltip, no stack count.
     *
     * @param size the width and height the item is drawn at
     */
    record Item(ItemStack icon, Component text, int size) implements DialogBlock {
        public Item {
            if (icon.isEmpty() || size < MIN_ITEM_SIZE || size > MAX_ITEM_SIZE) {
                throw new IllegalArgumentException("An item block needs an item drawn between " + MIN_ITEM_SIZE + " and " + MAX_ITEM_SIZE + " pixels");
            }
            icon = icon.copy();
        }
    }
}
