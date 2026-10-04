package com.panita.enriquecraft.core.ui;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/**
 * Text that copies a value to the clipboard when clicked in the client companion, such as an id. The server
 * only marks the text; the client does the copying and honors no other click event. Anywhere else, such as a
 * chest, the text is plain text.
 */
public final class CopyText {

    private CopyText() {
    }

    /** The text, marked so that a click copies {@code value}. */
    public static Component of(Component text, String value) {
        return text.copy().withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(value)));
    }
}
