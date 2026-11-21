package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One line of text in a detail view. The parts of it the server marked with a copy-to-clipboard click event,
 * such as an id, copy that value when clicked; they are underlined and show a pointing hand when hovered, and
 * the rest of the line is plain text. Only that one event is honored: the client never runs anything else a
 * server could attach to text.
 */
final class UiLineWidget extends AbstractWidget {

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final long FEEDBACK_MILLIS = 1500;

    private static final Component COPY_HINT = Component.translatable("enriquecraft.ui.copy");
    private static final Component COPIED = Component.translatable("enriquecraft.ui.copied");

    /** A stretch of the line that copies {@code value}, in pixels from the start of the line. */
    private record Copyable(int start, int end, String value) {
    }

    private final Font font;
    private final Component line;
    private final List<Copyable> copyables;
    private long copiedAt = Long.MIN_VALUE / 2;

    UiLineWidget(Font font, Component line) {
        super(0, 0, font.width(line), font.lineHeight, line);
        this.font = font;
        this.line = line;
        this.copyables = copyablesOf(font, line);
        // A line with nothing to copy is inert, like plain text.
        this.active = !copyables.isEmpty();
    }

    /** Measures each marked part by the width of everything before it, pictures in the text included. */
    private static List<Copyable> copyablesOf(Font font, Component line) {
        List<Copyable> found = new ArrayList<>();
        measure(font, line, Style.EMPTY, new int[]{0}, found);
        return List.copyOf(found);
    }

    /**
     * Walks the text in the order it is drawn. Each part is measured alone, with only its own content, so a picture
     * keeps its real width (flattening the text would turn it into a placeholder character).
     */
    private static void measure(Font font, Component part, Style inherited, int[] x, List<Copyable> found) {
        Style style = part.getStyle().applyTo(inherited);
        int width = font.width(MutableComponent.create(part.getContents()).withStyle(style));
        if (style.getClickEvent() instanceof ClickEvent.CopyToClipboard copy && width > 0) {
            Copyable last = found.isEmpty() ? null : found.getLast();
            if (last != null && last.end() == x[0] && last.value().equals(copy.value())) {
                found.set(found.size() - 1, new Copyable(last.start(), x[0] + width, last.value()));
            } else {
                found.add(new Copyable(x[0], x[0] + width, copy.value()));
            }
        }
        x[0] += width;
        for (Component sibling : part.getSiblings()) {
            measure(font, sibling, style, x, found);
        }
    }

    private Optional<Copyable> copyableAt(double mouseX) {
        double local = mouseX - getX();
        return copyables.stream().filter(part -> local >= part.start() && local < part.end()).findFirst();
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return button.button() == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        copyableAt(event.x()).ifPresent(part -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(part.value());
            copiedAt = System.currentTimeMillis();
        });
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Clicking copies silently; the tooltip confirms it.
    }

    /** The hand is requested only over the marked part, not over the whole line. */
    @Override
    protected void handleCursor(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, line, getX(), getY(), TEXT_COLOR);
        if (!active || !isHovered()) {
            return;
        }
        copyableAt(mouseX).ifPresent(part -> {
            graphics.fill(getX() + part.start(), getY() + font.lineHeight, getX() + part.end(), getY() + font.lineHeight + 1,
                    TEXT_COLOR);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
            boolean justCopied = System.currentTimeMillis() - copiedAt < FEEDBACK_MILLIS;
            graphics.setTooltipForNextFrame(font, justCopied ? COPIED : COPY_HINT, mouseX, mouseY);
        });
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
