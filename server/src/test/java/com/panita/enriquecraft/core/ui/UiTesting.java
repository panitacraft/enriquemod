package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.gui.MenuTesting;
import com.panita.enriquecraft.core.network.UiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets tests show a {@link UiMenu} as a chest and look at its slots without a player. Lives in this
 * package to reach what is package-private, so production code stays as narrow as it is.
 */
public final class UiTesting {

    private UiTesting() {
    }

    /** Draws the menu as the chest a vanilla client would get. */
    public static void drawAsChest(UiMenu menu) {
        UiChestMenu chest = new UiChestMenu(menu);
        menu.showingAs(chest);
        MenuTesting.draw(chest);
    }

    /** What a chest slot currently shows, or null when it is empty. */
    public static MenuItem itemAt(UiMenu menu, int slot) {
        return MenuTesting.itemAt(menu.chest(), slot);
    }

    /** Presses the chest slot as an ordinary left click, without a player. */
    public static void click(UiMenu menu, int slot) {
        MenuTesting.click(menu.chest(), slot);
    }

    /** The number of rows of the chest the menu was drawn as. */
    public static int rows(UiMenu menu) {
        return new UiChestMenu(menu).rows();
    }

    /** Types a value into the only text field of the menu, as a player confirming it would. */
    public static void submit(UiMenu menu, String text) {
        UiInputHandler handler = menu.layout(0).inputs().values().iterator().next();
        handler.action().accept(new UiSubmit(null, text));
    }

    /** Chooses an option of the only dropdown of the menu, as a player picking it would. */
    public static void select(UiMenu menu, int option) {
        UiSelectHandler handler = menu.layout(0).selects().values().iterator().next();
        handler.action().accept(new UiSelect(null, option));
    }

    /** Drags the draggable button at one position (counting only draggable ones, in screen order) onto another. */
    public static void dragOnto(UiMenu menu, int draggedIndex, int targetIndex) {
        UiLayout layout = menu.layout(0);
        List<Integer> ids = new ArrayList<>();
        collectDraggable(layout.root(), ids);
        layout.drops().accept(new UiDrop(null, ids.get(draggedIndex), ids.get(targetIndex)));
    }

    private static void collectDraggable(UiElement element, List<Integer> ids) {
        switch (element) {
            case UiElement.Column column -> column.children().forEach(child -> collectDraggable(child, ids));
            case UiElement.Row row -> row.children().forEach(child -> collectDraggable(child, ids));
            case UiElement.Grid grid -> grid.children().forEach(child -> collectDraggable(child, ids));
            case UiElement.Button button -> {
                if (button.draggable()) {
                    ids.add(button.id());
                }
            }
            default -> { }
        }
    }

    /** Presses the button with the given id in the menu's current description, as a left click without a player. */
    public static void press(UiMenu menu, int buttonId) {
        menu.layout(0).handlers().get(buttonId).accept(new UiClick(null, 0, false));
    }

    /** The tree the menu currently describes. */
    public static UiElement root(UiMenu menu) {
        return menu.layout(0).root();
    }
}
