package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Turns a screen description into vanilla layouts and widgets. The client sizes everything with its
 * own font and GUI scale, so text never overflows whatever the server sent.
 */
final class UiLayouts {

    private static final int SPACING = 4;
    private static final int GRID_SPACING = 3;
    private static final int GAP = 12;

    /** Where an element sits, since a spacer and a button mean something different in each place. */
    private enum Place {
        COLUMN, ROW, GRID
    }

    private final Font font;
    private final UiActions actions;

    UiLayouts(Font font, UiActions actions) {
        this.font = font;
        this.actions = actions;
    }

    LayoutElement build(UiElement element) {
        return build(element, Place.COLUMN);
    }

    /** A row of elements side by side, such as the actions of a footer. */
    LinearLayout row(List<UiElement> children) {
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

    private LayoutElement build(UiElement element, Place place) {
        return switch (element) {
            case UiElement.Column column -> column(column.children());
            case UiElement.Row row -> row(row.children());
            case UiElement.Grid grid -> grid(grid);
            case UiElement.Label label -> text(label.text());
            case UiElement.Page page -> text(Component.literal(page.page() + " / " + page.pages()));
            case UiElement.TextInput input -> new UiInputWidget(font, input, actions);
            case UiElement.Button button -> place == Place.GRID
                    ? UiButtonWidget.cell(font, button, actions)
                    : UiButtonWidget.action(font, button, actions);
            // Grid cells keep their size so the grid stays aligned; elsewhere a spacer is just a gap.
            case UiElement.Spacer ignored -> place == Place.GRID ? new UiCellWidget() : SpacerElement.height(SPACING);
        };
    }

    StringWidget text(Component text) {
        // Only the default: a text that carries its own colors keeps them.
        return new StringWidget(text.copy().withStyle(style -> style.withColor(UiTheme.MUTED & 0xFFFFFF)), font);
    }

    private LinearLayout column(List<UiElement> children) {
        LinearLayout column = LinearLayout.vertical().spacing(SPACING);
        children.forEach(child -> column.addChild(build(child, Place.COLUMN), settings -> settings.alignHorizontallyCenter()));
        return column;
    }

    /**
     * A label inside a grid, such as "nothing to show", is not a cell: it is drawn across the middle
     * of the grid instead, so it cannot widen a column.
     */
    private LayoutElement grid(UiElement.Grid grid) {
        GridLayout layout = new GridLayout().spacing(GRID_SPACING);
        UiElement.Label overlay = null;
        int cells = grid.columns() * grid.rows();
        for (int index = 0; index < cells; index++) {
            UiElement child = index < grid.children().size() ? grid.children().get(index) : new UiElement.Spacer();
            if (child instanceof UiElement.Label label) {
                overlay = label;
                child = new UiElement.Spacer();
            }
            layout.addChild(build(child, Place.GRID), index / grid.columns(), index % grid.columns());
        }
        if (overlay == null) {
            return layout;
        }
        FrameLayout frame = new FrameLayout();
        frame.addChild(layout);
        frame.addChild(text(overlay.text()));
        return frame;
    }
}
