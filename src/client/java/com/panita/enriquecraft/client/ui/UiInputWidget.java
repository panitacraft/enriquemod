package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;

/**
 * A text field of a server-described screen. Enter sends the value to the server; nothing is sent
 * while the player is still typing.
 */
final class UiInputWidget extends EditBox {

    private static final int WIDTH = 140;

    private final int id;
    private final UiActions actions;

    UiInputWidget(Font font, UiElement.TextInput input, UiActions actions) {
        super(font, 0, 0, WIDTH, UiButtonWidget.SIZE, input.hint());
        this.id = input.id();
        this.actions = actions;
        setMaxLength(input.maxLength());
        setValue(input.value());
        setHint(input.hint());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isFocused() && event.isConfirmation()) {
            actions.submit(id, getValue());
            return true;
        }
        return super.keyPressed(event);
    }
}
