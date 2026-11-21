package com.panita.enriquecraft.core.ui;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads back which parts of a line copy something when clicked. */
public final class CopyTextTesting {

    private CopyTextTesting() {
    }

    /** For each part of the line that copies, "what it shows -> what it copies", in order. */
    public static List<String> clickable(Component line) {
        List<String> parts = new ArrayList<>();
        line.visit((style, text) -> {
            if (style.getClickEvent() instanceof ClickEvent.CopyToClipboard copy) {
                parts.add(text + " -> " + copy.value());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return parts;
    }
}
