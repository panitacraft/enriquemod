package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Creates the interactive elements of one description of a screen. It gives every button an id and
 * remembers what the button does, so the client only ever has to say which one was pressed.
 * <p>
 * Ids keep counting across the descriptions of one showing of a screen, so a press that was already
 * on its way when the screen changed can never land on a different button.
 */
public final class UiBuilder {

    private final Map<Integer, Consumer<UiClick>> handlers = new HashMap<>();
    private int nextId;

    UiBuilder(int firstId) {
        this.nextId = firstId;
    }

    /**
     * @param icon    shown on the button; use {@link ItemStack#EMPTY} for none
     * @param label   the button's name
     * @param tooltip extra lines shown when hovering the button
     * @param action  runs on the server when the button is pressed
     */
    public UiElement.Button button(ItemStack icon, Component label, List<Component> tooltip, Consumer<UiClick> action) {
        int id = nextId++;
        handlers.put(id, action);
        return new UiElement.Button(id, icon, label, tooltip);
    }

    UiLayout build(UiElement root) {
        return new UiLayout(root, handlers, nextId);
    }
}
