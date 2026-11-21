package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * The title of a detail view that the player can edit in place. It reads as a title, in the title font;
 * clicking it turns it into a text field holding the value, and the screen shows a check beside it as soon
 * as the value differs. Enter or the check sends the value to the server, Escape drops the change.
 */
final class UiTitleEditWidget extends EditBox {

    private static final int MIN_WIDTH = 150;
    private static final int INSET = 6;
    private static final int UNDERLINE_GAP = 2;
    private static final Component EDIT_HINT = Component.translatable("enriquecraft.ui.edit");

    private final Font font;
    private final int id;
    private final Component title;
    private final float scale;
    private final String original;
    private final UiActions actions;
    private Consumer<Boolean> dirtyListener = dirty -> { };

    UiTitleEditWidget(Font font, Component title, UiElement.TextInput input, float scale, UiActions actions) {
        super(font, 0, 0, Math.max(MIN_WIDTH, Math.round(font.width(title) * scale) + 2 * INSET),
                Math.round(font.lineHeight * scale) + 6, input.hint());
        this.font = font;
        this.id = input.id();
        this.title = title.copy().withStyle(style -> style.withFont(UiTheme.TITLE_FONT));
        this.scale = scale;
        this.original = input.value();
        this.actions = actions;
        setBordered(false);
        setTextColor(UiTheme.TEXT);
        setMaxLength(input.maxLength());
        setValue(input.value());
        setResponder(value -> dirtyListener.accept(!value.equals(original)));
    }

    /** Told whether the value differs from the one the screen opened with, so a check can appear. */
    void onDirty(Consumer<Boolean> listener) {
        this.dirtyListener = listener;
    }

    void submit() {
        actions.submit(id, getValue());
    }

    /** Puts the original value back and stops editing. */
    void cancel() {
        setValue(original);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!isFocused()) {
            drawTitle(graphics);
            if (isHovered()) {
                graphics.setTooltipForNextFrame(font, EDIT_HINT, mouseX, mouseY);
            }
            return;
        }
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        UiTheme.pill(graphics, x, y, width, height, UiTheme.CELL_EMPTY, UiTheme.ACCENT);
        setX(x + INSET);
        setY(y + (height - font.lineHeight) / 2);
        setWidth(width - 2 * INSET);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        setX(x);
        setY(y);
        setWidth(width);
    }

    /** The server's title, centered, with a line under it while hovered that says it can be clicked. */
    private void drawTitle(GuiGraphicsExtractor graphics) {
        int textWidth = Math.round(font.width(title) * scale);
        int textHeight = Math.round(font.lineHeight * scale);
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + (getHeight() - textHeight) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, textY);
        graphics.pose().scale(scale, scale);
        graphics.text(font, title, 0, 0, UiTheme.TEXT);
        graphics.pose().popMatrix();
        if (isHovered()) {
            graphics.fill(textX, textY + textHeight + UNDERLINE_GAP, textX + textWidth, textY + textHeight + UNDERLINE_GAP + 1,
                    UiTheme.ACCENT);
        }
    }

    /** The text sits inset from the frame, so a click must be measured from where the text starts. */
    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(new MouseButtonEvent(event.x() - INSET, event.y(), event.buttonInfo()), doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isFocused() && event.isConfirmation()) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Drops focus when the player was editing, so the screen can tell Escape apart from going back. */
    boolean stopEditing() {
        if (!isFocused()) {
            return false;
        }
        cancel();
        Minecraft.getInstance().gui.screen().setFocused(null);
        return true;
    }
}
