package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a screen description into vanilla layouts and widgets. The client sizes everything with its
 * own font and GUI scale, so text never overflows whatever the server sent.
 */
final class UiLayouts {

    private static final int SPACING = 4;
    private static final int GRID_SPACING = 3;
    private static final int GAP = 12;
    private static final int DETAIL_ICON = UiButtonWidget.BIG_ICON;
    // A scroll area never takes more than this share of the window, whatever the server asked for.
    private static final double MAX_SCROLL_SHARE = 0.45;

    /** Where an element sits, since a spacer and a button mean something different in each place. */
    private enum Place {
        COLUMN, ROW, GRID, FOOTER
    }

    private final Font font;
    private final UiActions actions;
    private final List<UiDropdownWidget> dropdowns = new ArrayList<>();

    UiLayouts(Font font, UiActions actions) {
        this.font = font;
        this.actions = actions;
    }

    LayoutElement build(UiElement element) {
        return build(element, Place.COLUMN);
    }

    /** The dropdowns built so far; the screen draws their lists above everything else. */
    List<UiDropdownWidget> dropdowns() {
        return dropdowns;
    }

    /** A row of elements side by side, such as the fields of a footer. */
    LinearLayout row(List<UiElement> children) {
        return row(children, Place.ROW);
    }

    /** The actions of a footer: buttons with an item show only the item, with the label as a tooltip. */
    LinearLayout footerActions(List<UiElement> children) {
        return row(children, Place.FOOTER);
    }

    private LinearLayout row(List<UiElement> children, Place place) {
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
            row.addChild(build(child, place), settings -> settings.alignVerticallyMiddle());
            gap = false;
            any = true;
        }
        return row;
    }

    private LayoutElement build(UiElement element, Place place) {
        return switch (element) {
            case UiElement.Column column -> column(column.children());
            case UiElement.Row row -> row(row.children(), Place.ROW);
            case UiElement.Grid grid -> grid(grid);
            case UiElement.Scroll scroll -> scroll(scroll);
            case UiElement.Label label -> text(label.text());
            case UiElement.Page page -> text(Component.literal(page.page() + " / " + page.pages()));
            case UiElement.Detail detail -> detail(detail);
            case UiElement.TextInput input -> new UiInputWidget(font, input, actions);
            case UiElement.Dropdown dropdown -> dropdown(dropdown);
            case UiElement.Divider ignored -> new UiDividerWidget();
            case UiElement.Button button -> switch (place) {
                case GRID -> UiButtonWidget.cell(font, button, actions);
                case FOOTER -> button.icon().isEmpty() || button.role() == ButtonRole.CONFIRM
                        ? UiButtonWidget.action(font, button, actions)
                        : UiButtonWidget.cell(font, button, actions);
                default -> UiButtonWidget.action(font, button, actions);
            };
            // Grid cells keep their size so the grid stays aligned; elsewhere a spacer is just a gap.
            case UiElement.Spacer ignored -> place == Place.GRID ? new UiCellWidget() : SpacerElement.height(SPACING);
        };
    }

    StringWidget text(Component text) {
        // Only the default: a text that carries its own colors keeps them.
        return new StringWidget(text.copy().withStyle(style -> style.withColor(UiTheme.MUTED & 0xFFFFFF)), font);
    }

    private UiDropdownWidget dropdown(UiElement.Dropdown dropdown) {
        UiDropdownWidget widget = new UiDropdownWidget(font, dropdown, actions);
        dropdowns.add(widget);
        return widget;
    }

    /** The content with a scroll bar when it is taller than the space it may take. */
    private LayoutElement scroll(UiElement.Scroll scroll) {
        LinearLayout content = LinearLayout.vertical();
        content.addChild(build(scroll.content(), Place.COLUMN));
        content.arrangeElements();
        int windowShare = (int) (Minecraft.getInstance().getWindow().getGuiScaledHeight() * MAX_SCROLL_SHARE);
        int height = Math.min(scroll.maxHeight(), windowShare);
        ScrollableLayout scrollable = new ScrollableLayout(Minecraft.getInstance(), content, height);
        scrollable.arrangeElements();
        return scrollable;
    }

    /** The item large, its name under it, and the lines about it, all centered. */
    private LinearLayout detail(UiElement.Detail detail) {
        LinearLayout column = LinearLayout.vertical().spacing(SPACING);
        if (!detail.icon().isEmpty()) {
            LayoutElement icon = detail.iconId() == UiElement.Detail.NOT_PRESSABLE
                    ? new UiItemWidget(detail.icon(), DETAIL_ICON)
                    : UiButtonWidget.big(font, detail.icon(), detail.iconTooltip(),
                            (mouse, shift) -> actions.press(detail.iconId(), mouse, shift));
            column.addChild(icon, settings -> settings.alignHorizontallyCenter());
        }
        column.addChild(new UiTitleWidget(font, detail.title(), 1.25F), settings -> settings.alignHorizontallyCenter().paddingVertical(4));
        detail.lines().forEach(line -> column.addChild(new StringWidget(line, font), settings -> settings.alignHorizontallyCenter()));
        return column;
    }

    private LinearLayout column(List<UiElement> children) {
        LinearLayout column = LinearLayout.vertical().spacing(SPACING);
        List<UiDividerWidget> dividers = new ArrayList<>();
        for (UiElement child : children) {
            LayoutElement built = build(child, Place.COLUMN);
            if (built instanceof UiDividerWidget divider) {
                dividers.add(divider);
            }
            column.addChild(built, settings -> settings.alignHorizontallyCenter());
        }
        // A divider spans the column, so it is as wide as the widest thing beside it.
        column.arrangeElements();
        int width = column.getWidth();
        dividers.forEach(divider -> divider.setWidth(width));
        column.arrangeElements();
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
