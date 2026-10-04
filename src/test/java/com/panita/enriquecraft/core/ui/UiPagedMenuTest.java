package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A paged menu shown as a chest must look and behave exactly like the classic paginated chest menu,
 * since that is what players without the client companion keep seeing.
 */
class UiPagedMenuTest {

    private static final int PREVIOUS = 48;
    private static final int CLOSE = 49;
    private static final int NEXT = 50;
    private static final int BACK = 45;
    private static final int FIRST_CONTENT = 10;

    /** A menu of plain numbered entries; pressing an entry records it. */
    private static final class NumbersMenu extends UiPagedMenu<Integer> {
        private final List<Integer> numbers = new ArrayList<>();
        private final List<Integer> pressed = new ArrayList<>();

        NumbersMenu(UiService ui, UiMenu previous, int count) {
            super(ui, previous);
            IntStream.range(0, count).forEach(numbers::add);
        }

        @Override
        protected Component title() {
            return Component.literal("Numbers");
        }

        @Override
        protected List<Integer> entries() {
            return numbers;
        }

        @Override
        protected UiElement render(UiBuilder builder, Integer entry) {
            return builder.button(new ItemStack(Items.STONE), Component.literal("N" + entry), List.of(),
                    click -> pressed.add(entry));
        }
    }

    @TempDir
    Path directory;

    private UiService ui;
    private Item filler;

    @BeforeEach
    void createService() {
        ui = MinecraftTestSupport.uiService(directory);
        filler = ui.factory().item(Items.STAINED_GLASS_PANE.black()).build().getItem();
    }

    private NumbersMenu menu(int count) {
        NumbersMenu menu = new NumbersMenu(ui, null, count);
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static String nameAt(UiMenu menu, int slot) {
        MenuItem item = UiTesting.itemAt(menu, slot);
        return item == null ? null : item.stack().get(DataComponents.CUSTOM_NAME).getString();
    }

    private static Item itemAt(UiMenu menu, int slot) {
        MenuItem item = UiTesting.itemAt(menu, slot);
        return item == null ? null : item.stack().getItem();
    }

    @Test
    void isSixRowsInsideAFrame() {
        NumbersMenu menu = menu(3);

        assertEquals(6, UiTesting.rows(menu));
        assertEquals(filler, itemAt(menu, 0));
        assertEquals(filler, itemAt(menu, 8));
        assertEquals(filler, itemAt(menu, 9));
        assertEquals(filler, itemAt(menu, 53));
    }

    @Test
    void entriesFillTheInteriorInOrder() {
        NumbersMenu menu = menu(3);

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
        assertEquals("N1", nameAt(menu, FIRST_CONTENT + 1));
        assertEquals("N2", nameAt(menu, FIRST_CONTENT + 2));
        assertEquals(Items.STONE, itemAt(menu, FIRST_CONTENT + 2));
        assertNull(UiTesting.itemAt(menu, FIRST_CONTENT + 3), "unused interior slots stay empty");
    }

    @Test
    void entriesWrapAtTheEndOfTheInteriorRow() {
        NumbersMenu menu = menu(10);

        assertEquals("N6", nameAt(menu, 16));
        assertEquals("N7", nameAt(menu, 19));
        assertEquals(filler, itemAt(menu, 18));
    }

    @Test
    void onlyTwentyEightEntriesShowOnAPage() {
        NumbersMenu menu = menu(40);

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
        assertEquals("N27", nameAt(menu, 43));
        assertEquals(Items.ARROW, itemAt(menu, NEXT));
    }

    @Test
    void firstPageHasNoPreviousButton() {
        assertEquals(filler, itemAt(menu(40), PREVIOUS));
    }

    @Test
    void nextShowsTheFollowingPageAndPreviousComesBack() {
        NumbersMenu menu = menu(40);

        UiTesting.click(menu, NEXT);

        assertEquals("N28", nameAt(menu, FIRST_CONTENT));
        assertEquals("N39", nameAt(menu, 10 + 11 / 7 * 9 + 11 % 7));
        assertEquals(Items.ARROW, itemAt(menu, PREVIOUS));
        assertEquals(filler, itemAt(menu, NEXT), "the last page has no next button");

        UiTesting.click(menu, PREVIOUS);

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
    }

    @Test
    void closeButtonShowsThePageIndicator() {
        NumbersMenu menu = menu(40);
        UiTesting.click(menu, NEXT);

        MenuItem close = UiTesting.itemAt(menu, CLOSE);

        assertEquals(Items.BARRIER, close.stack().getItem());
        assertEquals("Página 2 de 2", close.stack().get(DataComponents.LORE).lines().getFirst().getString());
    }

    @Test
    void pressingAnEntryRunsItsAction() {
        NumbersMenu menu = menu(40);

        UiTesting.click(menu, FIRST_CONTENT + 2);
        UiTesting.click(menu, NEXT);
        UiTesting.click(menu, FIRST_CONTENT);

        assertEquals(List.of(2, 28), menu.pressed);
    }

    @Test
    void pressingTheFrameOrAnEmptySlotDoesNothing() {
        NumbersMenu menu = menu(3);

        UiTesting.click(menu, 0);
        UiTesting.click(menu, FIRST_CONTENT + 20);
        UiTesting.click(menu, 999);
        UiTesting.click(menu, -999);

        assertTrue(menu.pressed.isEmpty());
    }

    @Test
    void emptyListShowsTheEmptyMarkerInTheMiddle() {
        NumbersMenu menu = menu(0);

        assertEquals(Items.PAPER, itemAt(menu, 22));
        assertNull(UiTesting.itemAt(menu, FIRST_CONTENT));
    }

    @Test
    void pageIsClampedWhenEntriesDisappear() {
        NumbersMenu menu = menu(40);
        UiTesting.click(menu, NEXT);
        menu.numbers.subList(10, 40).clear();

        menu.refresh();

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
        assertEquals(filler, itemAt(menu, PREVIOUS));
        assertEquals(filler, itemAt(menu, NEXT));
    }

    @Test
    void backButtonExistsOnlyWhenThereIsAPreviousMenu() {
        NumbersMenu alone = menu(3);
        NumbersMenu nested = new NumbersMenu(ui, alone, 3);
        UiTesting.drawAsChest(nested);

        assertEquals(filler, itemAt(alone, BACK));
        assertEquals(Items.OAK_DOOR, itemAt(nested, BACK));
    }
}
