package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Creates the interactive elements of one description of a screen. It gives every button and field
 * an id and remembers what it does, so the client only ever has to say which one was used.
 * <p>
 * Ids keep counting across the descriptions of one showing of a screen, so a press that was already
 * on its way when the screen changed can never land on a different element.
 */
public final class UiBuilder {

    private final Map<Integer, Consumer<UiClick>> handlers = new HashMap<>();
    private final Map<Integer, UiInputHandler> inputs = new HashMap<>();
    private final Map<Integer, UiSelectHandler> selects = new HashMap<>();
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
        return button(ButtonRole.NONE, icon, label, tooltip, action);
    }

    /**
     * A button with a standard purpose, such as closing the screen or turning a page. The icon, label
     * and tooltip are what a chest shows; the client companion draws the button its own way.
     */
    public UiElement.Button button(ButtonRole role, ItemStack icon, Component label, List<Component> tooltip,
                                   Consumer<UiClick> action) {
        return button(role, icon, label, tooltip, "", action);
    }

    /**
     * A button with a short mark drawn over its item, such as a check. A chest, which cannot draw over an
     * item, puts the mark before the name instead.
     */
    public UiElement.Button button(ButtonRole role, ItemStack icon, Component label, List<Component> tooltip,
                                   String badge, Consumer<UiClick> action) {
        int id = nextId++;
        handlers.put(id, action);
        return new UiElement.Button(id, role, icon, label, tooltip, badge);
    }

    /**
     * One thing shown in full whose item can be pressed, for example to change it.
     *
     * @param tooltip what pressing the item does, shown when hovering it
     * @param action  runs on the server when the item is pressed
     */
    public UiElement.Detail detail(ItemStack icon, Component title, List<Component> lines, List<Component> tooltip,
                                   Consumer<UiClick> action) {
        int id = nextId++;
        handlers.put(id, action);
        return new UiElement.Detail(icon, title, lines, id, tooltip);
    }

    /**
     * A choice among a few options. Choosing one runs the action with its position; the menu is then
     * expected to refresh itself so the new choice shows.
     *
     * @param selected the position of the option chosen now
     */
    public UiElement.Dropdown dropdown(Component label, List<Component> options, int selected, Consumer<UiSelect> action) {
        int id = nextId++;
        selects.put(id, new UiSelectHandler(options.size(), action));
        return new UiElement.Dropdown(id, label, options, selected);
    }

    /**
     * A field the player types a value into. Players without the client companion type it in chat
     * instead, so the action must not assume anything about how the value arrived.
     *
     * @param hint      names what the value is for
     * @param value     what the field holds now
     * @param maxLength the longest value the action accepts, at most {@link UiElement.TextInput#MAX_LENGTH}
     * @param action    runs on the server with the confirmed value; an empty value means the field was cleared
     */
    public UiElement.TextInput input(Component hint, String value, int maxLength, Consumer<UiSubmit> action) {
        int id = nextId++;
        inputs.put(id, new UiInputHandler(maxLength, action));
        return new UiElement.TextInput(id, hint, value, maxLength);
    }

    UiLayout build(UiElement root) {
        return new UiLayout(root, handlers, inputs, selects, nextId);
    }
}
