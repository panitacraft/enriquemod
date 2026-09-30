package com.panita.enriquecraft.core.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginatedMenuTest {

    private static final int PREVIOUS = 48;
    private static final int CLOSE = 49;
    private static final int NEXT = 50;
    private static final int BACK = 45;
    private static final int FIRST_CONTENT = 10;

    /** A menu of plain numbered entries; clicking an entry records it. */
    private static final class NumbersMenu extends PaginatedMenu<Integer> {
        private final List<Integer> numbers = new ArrayList<>();
        private final List<Integer> clicked = new ArrayList<>();

        NumbersMenu(MenuFactory factory, Menu previous, int count) {
            super(factory, previous);
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
        protected MenuItem render(Integer entry) {
            return MenuItem.button(factory().item(Items.STONE).name("N" + entry).build(), click -> clicked.add(entry));
        }
    }

    @TempDir
    Path directory;

    private MenuFactory factory;

    @BeforeEach
    void createFactory() {
        factory = MinecraftTestSupport.menuFactory(directory);
    }

    /** The slot that shows the entry at this position of a page. */
    private static int contentSlot(int position) {
        return MenuFrame.interiorSlots(6).get(position);
    }

    private static void click(Menu menu, int slot) {
        menu.handleClick(new MenuClick(null, slot, 0, ContainerInput.PICKUP));
    }

    private static String nameAt(Menu menu, int slot) {
        MenuItem item = menu.itemAt(slot);
        return item == null ? null : item.stack().get(DataComponents.CUSTOM_NAME).getString();
    }

    private static Item itemAt(Menu menu, int slot) {
        MenuItem item = menu.itemAt(slot);
        return item == null ? null : item.stack().getItem();
    }

    @Test
    void borderIsFilledWithTheFiller() {
        NumbersMenu menu = new NumbersMenu(factory, null, 3);
        menu.prepare();

        assertEquals(factory.filler().getItem(), itemAt(menu, 0));
        assertEquals(factory.filler().getItem(), itemAt(menu, 8));
        assertEquals(factory.filler().getItem(), itemAt(menu, 9));
        assertEquals(factory.filler().getItem(), itemAt(menu, 53));
    }

    @Test
    void entriesFillTheInteriorInOrder() {
        NumbersMenu menu = new NumbersMenu(factory, null, 3);
        menu.prepare();

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
        assertEquals("N1", nameAt(menu, FIRST_CONTENT + 1));
        assertEquals("N2", nameAt(menu, FIRST_CONTENT + 2));
        assertEquals(Items.STONE, itemAt(menu, FIRST_CONTENT + 2));
        assertNull(menu.itemAt(FIRST_CONTENT + 3), "unused interior slots stay empty");
    }

    @Test
    void onlyTwentyEightEntriesShowOnAPage() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
        assertEquals("N27", nameAt(menu, 43));
        assertNotNull(menu.itemAt(NEXT));
        assertEquals(Items.ARROW, itemAt(menu, NEXT));
    }

    @Test
    void firstPageHasNoPreviousButton() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();

        assertEquals(factory.filler().getItem(), itemAt(menu, PREVIOUS));
    }

    @Test
    void nextShowsTheFollowingPageAndPreviousComesBack() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();

        click(menu, NEXT);

        assertEquals("N28", nameAt(menu, FIRST_CONTENT));
        assertEquals("N39", nameAt(menu, contentSlot(11)));
        assertNull(menu.itemAt(contentSlot(12)));
        assertEquals(Items.ARROW, itemAt(menu, PREVIOUS));
        assertEquals(factory.filler().getItem(), itemAt(menu, NEXT), "the last page has no next button");

        click(menu, PREVIOUS);

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
    }

    @Test
    void closeButtonShowsThePageIndicator() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();
        click(menu, NEXT);

        MenuItem close = menu.itemAt(CLOSE);

        assertEquals(Items.BARRIER, close.stack().getItem());
        String lore = close.stack().get(DataComponents.LORE).lines().get(0).getString();
        assertEquals("Página 2 de 2", lore);
    }

    @Test
    void clickingAnEntryRunsItsAction() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();

        click(menu, FIRST_CONTENT + 2);
        click(menu, NEXT);
        click(menu, FIRST_CONTENT);

        assertEquals(List.of(2, 28), menu.clicked);
    }

    @Test
    void clickingTheFrameOrAnEmptySlotDoesNothing() {
        NumbersMenu menu = new NumbersMenu(factory, null, 3);
        menu.prepare();

        click(menu, 0);
        click(menu, FIRST_CONTENT + 20);
        click(menu, 999);
        click(menu, -999);

        assertTrue(menu.clicked.isEmpty());
    }

    @Test
    void onlyOrdinaryClicksPressButtons() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();

        for (ContainerInput input : new ContainerInput[]{ContainerInput.SWAP, ContainerInput.THROW, ContainerInput.CLONE,
                ContainerInput.QUICK_CRAFT, ContainerInput.PICKUP_ALL}) {
            menu.handleClick(new MenuClick(null, NEXT, 0, input));
            menu.handleClick(new MenuClick(null, FIRST_CONTENT, 0, input));
        }

        assertEquals("N0", nameAt(menu, FIRST_CONTENT), "the page did not turn");
        assertTrue(menu.clicked.isEmpty());
        menu.handleClick(new MenuClick(null, FIRST_CONTENT, 0, ContainerInput.QUICK_MOVE));
        assertEquals(List.of(0), menu.clicked, "a shift click is an ordinary click");
    }

    @Test
    void emptyListShowsTheEmptyMarker() {
        NumbersMenu menu = new NumbersMenu(factory, null, 0);
        menu.prepare();

        assertEquals(Items.PAPER, itemAt(menu, 22));
        assertNull(menu.itemAt(FIRST_CONTENT));
    }

    @Test
    void pageIsClampedWhenEntriesDisappear() {
        NumbersMenu menu = new NumbersMenu(factory, null, 40);
        menu.prepare();
        click(menu, NEXT);
        menu.numbers.subList(10, 40).clear();

        menu.refresh();

        assertEquals("N0", nameAt(menu, FIRST_CONTENT));
        assertEquals(factory.filler().getItem(), itemAt(menu, PREVIOUS));
        assertEquals(factory.filler().getItem(), itemAt(menu, NEXT));
    }

    @Test
    void backButtonExistsOnlyWhenThereIsAPreviousMenu() {
        NumbersMenu alone = new NumbersMenu(factory, null, 3);
        alone.prepare();
        NumbersMenu nested = new NumbersMenu(factory, alone, 3);
        nested.prepare();

        assertEquals(factory.filler().getItem(), itemAt(alone, BACK));
        assertEquals(Items.OAK_DOOR, itemAt(nested, BACK));
    }
}
