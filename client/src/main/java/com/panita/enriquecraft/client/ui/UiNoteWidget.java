package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Lines of text drawn smaller than the rest of the screen, for a long text such as a description. Inert, like other text. */
final class UiNoteWidget extends AbstractWidget {

    private static final float SCALE = 0.75F;
    private static final int LINE_GAP = 1;

    private final Font font;
    private final List<Component> lines;

    UiNoteWidget(Font font, List<Component> lines) {
        super(0, 0, Math.round(lines.stream().mapToInt(font::width).max().orElse(0) * SCALE),
                Math.round(lines.size() * (font.lineHeight + LINE_GAP) * SCALE), Component.empty());
        this.font = font;
        this.lines = lines;
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(SCALE, SCALE);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), 0, index * (font.lineHeight + LINE_GAP), UiTheme.MUTED);
        }
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
