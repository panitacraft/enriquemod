package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A text field of a server-described screen, drawn in the screen's theme. Enter sends the value to
 * the server; nothing is sent while the player is still typing.
 */
final class UiInputWidget extends EditBox {

    private static final int WIDTH = 120;
    private static final int INSET = 6;

    private final Font font;
    private final int id;
    private final UiActions actions;

    UiInputWidget(Font font, UiElement.TextInput input, UiActions actions) {
        super(font, 0, 0, WIDTH, UiButtonWidget.GLYPH_SIZE + 2, input.hint());
        this.font = font;
        this.id = input.id();
        this.actions = actions;
        setBordered(false);
        setTextColor(UiTheme.TEXT);
        setMaxLength(input.maxLength());
        setValue(input.value());
        // A hint is a faint prompt, whatever color the server gave the label.
        setHint(Component.literal(input.hint().getString()).withStyle(style -> style.withColor(UiTheme.MUTED & 0xFFFFFF)));
    }

    /** The edit box has no frame of its own here: draw ours, and let the box draw its text inside it. */
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        UiTheme.pill(graphics, x, y, width, height, UiTheme.CELL_EMPTY, isFocused() ? UiTheme.ACCENT : UiTheme.CELL_BORDER);

        setX(x + INSET);
        setY(y + (height - font.lineHeight) / 2);
        setWidth(width - 2 * INSET);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        setX(x);
        setY(y);
        setWidth(width);
    }

    /** The text sits inset from the frame, so a click must be measured from where the text starts. */
    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(new MouseButtonEvent(event.x() - INSET, event.y(), event.buttonInfo()), doubleClick);
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
