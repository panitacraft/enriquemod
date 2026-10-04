package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A button of a server-described screen: an item icon and a label. A compact button shows only its
 * icon, and moves its label into the tooltip, so that many fit in a grid. Left and right clicks are
 * both reported, since the server decides what each of them does.
 */
final class UiButtonWidget extends AbstractButton {

    static final int SIZE = 20;
    private static final int ICON_SIZE = 16;
    private static final int PADDING = 4;
    private static final int MIN_WIDTH = 40;

    private final Font font;
    private final int id;
    private final ItemStack icon;
    private final boolean compact;
    private final ButtonPress press;

    UiButtonWidget(Font font, UiElement.Button button, boolean compact, ButtonPress press) {
        super(0, 0, width(font, button, compact), SIZE, button.label());
        this.font = font;
        this.id = button.id();
        this.icon = button.icon();
        this.compact = compact;
        this.press = press;

        List<Component> lines = new ArrayList<>();
        if (compact && !button.label().getString().isEmpty()) {
            lines.add(button.label());
        }
        lines.addAll(button.tooltip());
        if (!lines.isEmpty()) {
            setTooltip(Tooltip.create(joined(lines)));
        }
    }

    private static int width(Font font, UiElement.Button button, boolean compact) {
        if (compact) {
            return SIZE;
        }
        int iconSpace = button.icon().isEmpty() ? 0 : ICON_SIZE + PADDING;
        return Math.max(MIN_WIDTH, PADDING + iconSpace + font.width(button.label()) + PADDING);
    }

    private static Component joined(List<Component> lines) {
        MutableComponent text = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                text.append(Component.literal("\n"));
            }
            text.append(lines.get(index));
        }
        return text;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        int button = input instanceof MouseButtonEvent event ? event.button() : 0;
        press.press(id, button, input.hasShiftDown());
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return button.button() == 0 || button.button() == 1;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
        if (compact) {
            graphics.item(icon, getX() + (getWidth() - ICON_SIZE) / 2, iconY);
            return;
        }
        int textX = getX() + PADDING;
        if (!icon.isEmpty()) {
            graphics.item(icon, textX, iconY);
            textX += ICON_SIZE + PADDING;
        }
        graphics.text(font, getMessage(), textX, getY() + (getHeight() - font.lineHeight) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
