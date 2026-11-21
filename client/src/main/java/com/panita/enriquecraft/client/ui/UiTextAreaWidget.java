package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A multi-line text field of a server-described screen. The game's own multi-line box does the typing, the cursor
 * and the scrolling; this draws it in the screen's theme by turning its frame off and putting ours behind it.
 * Nothing is sent while the player types: the confirm button of the form sends the value, as it does for a one-line
 * field, and Enter adds a line.
 */
final class UiTextAreaWidget {

    private static final int BORDER = 1;
    private static final int PADDING = 4;
    private static final int MIN_WIDTH = 120;

    private final MultiLineEditBox box;
    private final Frame frame;
    private final FrameLayout layout;

    /**
     * @param maxWidth the widest the field may be, so it fits a narrow window whatever the server asked for
     */
    UiTextAreaWidget(Font font, UiElement.TextArea area, int maxWidth) {
        int width = Math.max(Math.min(MIN_WIDTH, area.width()), Math.min(area.width(), maxWidth));
        int height = area.lines() * font.lineHeight + 2 * PADDING + 2 * BORDER;
        // The game draws the scroll bar just outside the box, so the box leaves room for it inside our frame.
        int boxWidth = width - 2 * BORDER - AbstractScrollArea.SCROLLBAR_WIDTH;
        box = MultiLineEditBox.builder()
                .setPlaceholder(Component.literal(area.hint().getString()).withStyle(style -> style.withColor(UiTheme.MUTED & 0xFFFFFF)))
                .setTextColor(UiTheme.TEXT)
                .setTextShadow(false)
                .setShowBackground(false)
                .setShowDecorations(false)
                .build(font, boxWidth, height - 2 * BORDER, area.hint());
        box.setCharacterLimit(area.maxLength());
        box.setValue(area.value());
        frame = new Frame(width, height, box);
        layout = new FrameLayout(width, height);
        layout.addChild(frame);
        layout.addChild(box, settings -> settings.alignHorizontallyLeft().alignVerticallyTop().padding(BORDER));
    }

    LayoutElement layout() {
        return layout;
    }

    /** The widget that takes the keys, which the screen focuses so a form opens ready to type in. */
    AbstractWidget field() {
        return box;
    }

    String getValue() {
        return box.getValue();
    }

    /** The frame behind the box: inert, so every click reaches the box. */
    private static final class Frame extends AbstractWidget {

        private final MultiLineEditBox box;

        private Frame(int width, int height, MultiLineEditBox box) {
            super(0, 0, width, height, Component.empty());
            this.box = box;
            active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            UiTheme.pill(graphics, getX(), getY(), getWidth(), getHeight(), UiTheme.CELL_EMPTY,
                    box.isFocused() ? UiTheme.ACCENT : UiTheme.CELL_BORDER);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
