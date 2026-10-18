package com.panita.enriquecraft.client.chat;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/**
 * One line of chat that holds a slice of an image instead of text: it has no characters, so everything that measures or reads a
 * line of chat sees an empty one, and {@link ChatImageRenderer} draws the slice.
 */
record ChatImageLine(ChatImages.Ready image, int row) implements FormattedCharSequence {

    @Override
    public boolean accept(FormattedCharSink output) {
        return true;
    }
}
