package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;

import java.util.List;

/**
 * Turns a screen description into vanilla layouts and widgets. The client sizes everything with its
 * own font and GUI scale, so text never overflows whatever the server sent.
 */
final class UiLayouts {

    private static final int SPACING = 4;
    private static final int GRID_SPACING = 2;
    private static final int GAP = 12;

    /** Where an element sits, since a spacer means something different in each place. */
    private enum Place {
        COLUMN, ROW, GRID
    }

    private final Font font;
    private final ButtonPress press;

    UiLayouts(Font font, ButtonPress press) {
        this.font = font;
        this.press = press;
    }

    LayoutElement build(UiElement element) {
        return build(element, Place.COLUMN);
    }

    private LayoutElement build(UiElement element, Place place) {
        return switch (element) {
            case UiElement.Column column -> column(column.children());
            case UiElement.Row row -> row(row.children());
            case UiElement.Grid grid -> grid(grid);
            case UiElement.Label label -> new StringWidget(label.text(), font);
            case UiElement.Button button -> new UiButtonWidget(font, button, place == Place.GRID, press);
            // Grid cells keep their size so the grid stays aligned; elsewhere a spacer is just a gap.
            case UiElement.Spacer ignored -> place == Place.GRID
                    ? new SpacerElement(UiButtonWidget.SIZE, UiButtonWidget.SIZE)
                    : SpacerElement.height(SPACING);
        };
    }

    private LinearLayout column(List<UiElement> children) {
        LinearLayout column = LinearLayout.vertical().spacing(SPACING);
        children.forEach(child -> column.addChild(build(child, Place.COLUMN), settings -> settings.alignHorizontallyCenter()));
        return column;
    }

    /** Spacers at the ends are dropped and runs of them in between become one gap, so controls stay together. */
    private LinearLayout row(List<UiElement> children) {
        LinearLayout row = LinearLayout.horizontal().spacing(SPACING);
        boolean gap = false;
        boolean any = false;
        for (UiElement child : children) {
            if (child instanceof UiElement.Spacer) {
                gap = true;
                continue;
            }
            if (gap && any) {
                row.addChild(SpacerElement.width(GAP));
            }
            row.addChild(build(child, Place.ROW), settings -> settings.alignVerticallyMiddle());
            gap = false;
            any = true;
        }
        return row;
    }

    private GridLayout grid(UiElement.Grid grid) {
        GridLayout layout = new GridLayout().spacing(GRID_SPACING);
        int cells = grid.columns() * grid.rows();
        for (int index = 0; index < cells; index++) {
            LayoutElement cell = index < grid.children().size()
                    ? build(grid.children().get(index), Place.GRID)
                    : new SpacerElement(UiButtonWidget.SIZE, UiButtonWidget.SIZE);
            layout.addChild(cell, index / grid.columns(), index % grid.columns());
        }
        return layout;
    }
}
