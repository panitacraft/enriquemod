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
    private static final float TITLE_SCALE = 1.25F;
    // A scroll area never takes more than this share of the window, whatever the server asked for.
    private static final double MAX_SCROLL_SHARE = 0.45;
    private static final int MIN_SCROLL_HEIGHT = 30;
    private static final int COMPACT_ICON = 32;

    /** Where an element sits, since a spacer and a button mean something different in each place. */
    private enum Place {
        COLUMN, ROW, GRID, FOOTER
    }

    private final Font font;
    private final UiActions actions;
    private final List<UiDropdownWidget> dropdowns = new ArrayList<>();
    private final List<UiTitleEditWidget> titleEdits = new ArrayList<>();
    private final int scrollReduction;
    private final boolean compact;
    private boolean hasScroll;

    /**
     * @param scrollReduction how much shorter than they would be scroll areas should be built, to make the
     *                        screen fit the window
     * @param compact         whether to draw the large icons of detail views smaller, for a window that is
     *                        too small even with the scroll areas at their least
     */
    UiLayouts(Font font, UiActions actions, int scrollReduction, boolean compact) {
        this.compact = compact;
        this.font = font;
        this.actions = actions;
        this.scrollReduction = scrollReduction;
    }

    /** Whether anything built so far can give height back by scrolling. */
    boolean hasScroll() {
        return hasScroll;
    }

    LayoutElement build(UiElement element) {
        return build(element, Place.COLUMN);
    }

    /** The editable titles built so far; the screen lets Escape drop an edit in progress. */
    List<UiTitleEditWidget> titleEdits() {
        return titleEdits;
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
            // In a row, a pressable detail is a card; elsewhere it is the full view of one thing.
            case UiElement.Detail detail -> place == Place.ROW && detail.iconId() != UiElement.Detail.NOT_PRESSABLE
                    ? UiButtonWidget.card(font, detail.icon(), detail.title(), detail.iconTooltip(),
                            (mouse, shift) -> actions.press(detail.iconId(), mouse, shift))
                    : detail(detail);
            case UiElement.TextInput input -> new UiInputWidget(font, input, actions);
            case UiElement.Dropdown dropdown -> dropdown(dropdown);
            case UiElement.Divider ignored -> new UiDividerWidget();
            case UiElement.Button button -> switch (place) {
                case GRID -> UiButtonWidget.cell(font, button, actions);
                case FOOTER -> footerButton(button);
                default -> UiButtonWidget.action(font, button, actions);
            };
            // Grid cells keep their size so the grid stays aligned; elsewhere a spacer is just a gap.
            case UiElement.Spacer ignored -> place == Place.GRID ? new UiCellWidget() : SpacerElement.height(SPACING);
        };
    }

    /**
     * An answer is a colored pill with its label; an action is its item on a tinted surface, red when it
     * discards and green when it gives back, so no action looks like an item cell.
     */
    private UiButtonWidget footerButton(UiElement.Button button) {
        boolean withItem = !button.icon().isEmpty();
        return switch (button.role()) {
            case CONFIRM -> UiButtonWidget.labeled(font, button, UiTheme.Tone.SUCCESS, actions);
            case CANCEL -> UiButtonWidget.labeled(font, button, UiTheme.Tone.DANGER, actions);
            case DANGER -> withItem ? UiButtonWidget.tool(font, button, UiTheme.Tone.DANGER, actions)
                    : UiButtonWidget.labeled(font, button, UiTheme.Tone.DANGER, actions);
            case SUCCESS -> withItem ? UiButtonWidget.tool(font, button, UiTheme.Tone.SUCCESS, actions)
                    : UiButtonWidget.labeled(font, button, UiTheme.Tone.SUCCESS, actions);
            default -> withItem ? UiButtonWidget.tool(font, button, UiTheme.Tone.ACTION, actions)
                    : UiButtonWidget.action(font, button, actions);
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
        // The layout is exactly as tall as the limit it is given, so the limit must not exceed the content.
        hasScroll = true;
        int height = Math.min(content.getHeight(), Math.min(scroll.maxHeight(), windowShare));
        height = Math.min(height, Math.max(MIN_SCROLL_HEIGHT, height - scrollReduction));
        ScrollableLayout scrollable = new ScrollableLayout(Minecraft.getInstance(), content, height);
        scrollable.arrangeElements();
        return scrollable;
    }

    /** The item large, its name under it, and the lines about it, all centered. */
    private LinearLayout detail(UiElement.Detail detail) {
        LinearLayout column = LinearLayout.vertical().spacing(SPACING);
        if (!detail.icon().isEmpty()) {
            int size = compact ? COMPACT_ICON : DETAIL_ICON;
            LayoutElement icon = detail.iconId() == UiElement.Detail.NOT_PRESSABLE
                    ? new UiItemWidget(detail.icon(), size)
                    : UiButtonWidget.big(font, detail.icon(), size, detail.iconTooltip(),
                            (mouse, shift) -> actions.press(detail.iconId(), mouse, shift));
            column.addChild(icon, settings -> settings.alignHorizontallyCenter());
        }
        if (detail.editableTitle().isPresent()) {
            column.addChild(editableTitle(detail), settings -> settings.alignHorizontallyCenter().paddingVertical(4));
        } else if (!detail.title().getString().isEmpty()) {
            column.addChild(new UiTitleWidget(font, detail.title(), TITLE_SCALE), settings -> settings.alignHorizontallyCenter().paddingVertical(4));
        }
        detail.lines().forEach(line -> column.addChild(new UiLineWidget(font, line), settings -> settings.alignHorizontallyCenter()));
        return column;
    }

    /** A title that is a text field once clicked, with a check beside it that appears when the value changed. */
    private LayoutElement editableTitle(UiElement.Detail detail) {
        UiTitleEditWidget edit = new UiTitleEditWidget(font, detail.title(), detail.editableTitle().orElseThrow(), TITLE_SCALE, actions);
        titleEdits.add(edit);
        UiButtonWidget check = UiButtonWidget.check(font, Component.translatable("enriquecraft.ui.save"), (mouse, shift) -> edit.submit());
        check.visible = false;
        check.active = false;
        edit.onDirty(dirty -> {
            check.visible = dirty;
            check.active = dirty;
        });
        // The check sits at the edge of a frame as wide on both sides, so the title stays centered.
        FrameLayout frame = new FrameLayout(edit.getWidth() + 2 * (check.getWidth() + SPACING), edit.getHeight());
        frame.addChild(edit, settings -> settings.alignHorizontallyCenter().alignVerticallyMiddle());
        frame.addChild(check, settings -> settings.alignHorizontallyRight().alignVerticallyMiddle());
        return frame;
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
