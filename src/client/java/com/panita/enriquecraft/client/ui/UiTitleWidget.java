package com.panita.enriquecraft.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The title of a screen or a detail view, in the theme's title font. Inert, like the cells of a grid.
 */
final class UiTitleWidget extends AbstractWidget {

    private final Font font;
    private final Component text;
    private final float scale;

    UiTitleWidget(Font font, Component title, float scale) {
        super(0, 0, Math.round(font.width(styled(title)) * scale), Math.round(font.lineHeight * scale), title);
        this.font = font;
        this.text = styled(title);
        this.scale = scale;
        active = false;
    }

    private static Component styled(Component title) {
        return title.copy().withStyle(style -> style.withFont(UiTheme.TITLE_FONT));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, UiTheme.TEXT);
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
