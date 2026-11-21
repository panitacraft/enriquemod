package com.panita.enriquecraft.core.dialog;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/** A button of a dialog. Pressing it sends every field and this key back to the server. */
public record DialogButton(String key, Component label, Optional<Component> tooltip) {

    public DialogButton {
        DialogNames.require(key);
    }

    public DialogButton(String key, Component label) {
        this(key, label, Optional.empty());
    }
}
