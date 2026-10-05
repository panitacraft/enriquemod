package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The filter dropdown of a paged menu, next to the search box. */
class UiPagedMenuFilterTest {

    private static final int FILTER_SLOT = 47;
    private static final int FIRST_CONTENT = 10;

    /** A menu of words that can be narrowed to the ones starting with "a" or with "b". */
    private static final class WordsMenu extends UiPagedMenu<String> {
        private final List<String> words;
        private final List<Filter<String>> filters = new ArrayList<>();

        WordsMenu(UiService ui, List<String> words) {
            super(ui, null);
            this.words = words;
            filters.add(new Filter<>(Component.literal("Empieza con a"), word -> word.startsWith("a")));
            filters.add(new Filter<>(Component.literal("Empieza con b"), word -> word.startsWith("b")));
        }

        @Override
        protected Component title() {
            return Component.literal("Words");
        }

        @Override
        protected List<String> entries() {
            return words;
        }

        @Override
        protected Optional<Function<String, String>> searchText() {
            return Optional.of(Function.identity());
        }

        @Override
        protected List<Filter<String>> filters() {
            return filters;
        }

        @Override
        protected UiElement render(UiBuilder builder, String entry) {
            return builder.button(new ItemStack(Items.STONE), Component.literal(entry), List.of(), click -> { });
        }
    }

    @TempDir
    Path directory;

    private UiService ui;

    @BeforeEach
    void createService() {
        ui = MinecraftTestSupport.uiService(directory);
    }

    private WordsMenu menu(String... words) {
        WordsMenu menu = new WordsMenu(ui, List.of(words));
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static String nameAt(UiMenu menu, int slot) {
        MenuItem item = UiTesting.itemAt(menu, slot);
        return item == null ? null : item.stack().get(DataComponents.CUSTOM_NAME).getString();
    }

    @Test
    void aMenuWithFiltersHasTheDropdownBesideTheSearchBox() {
        WordsMenu menu = menu("alfa");

        assertEquals(Items.HOPPER, UiTesting.itemAt(menu, FILTER_SLOT).stack().getItem());
        assertEquals("Filtrar", nameAt(menu, FILTER_SLOT));
    }

    @Test
    void theDropdownOffersNoFilterFirstAndThenEachFilter() {
        WordsMenu menu = menu("alfa");

        List<String> lore = UiTesting.itemAt(menu, FILTER_SLOT).stack().get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList();

        assertEquals(List.of("▸ Sin filtro", "  Empieza con a", "  Empieza con b"), lore.subList(0, 3));
    }

    @Test
    void choosingAFilterKeepsOnlyWhatItAccepts() {
        WordsMenu menu = menu("alfa", "beta", "avion", "bola");

        UiTesting.select(menu, 2);

        assertEquals("beta", nameAt(menu, FIRST_CONTENT));
        assertEquals("bola", nameAt(menu, FIRST_CONTENT + 1));
        assertNull(UiTesting.itemAt(menu, FIRST_CONTENT + 2));
    }

    @Test
    void theChosenFilterIsMarkedInTheDropdown() {
        WordsMenu menu = menu("alfa", "beta");

        UiTesting.select(menu, 1);

        List<String> lore = UiTesting.itemAt(menu, FILTER_SLOT).stack().get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList();
        assertEquals("▸ Empieza con a", lore.get(1));
    }

    @Test
    void choosingNoFilterShowsEverythingAgain() {
        WordsMenu menu = menu("alfa", "beta");
        UiTesting.select(menu, 1);

        UiTesting.select(menu, 0);

        assertEquals("alfa", nameAt(menu, FIRST_CONTENT));
        assertEquals("beta", nameAt(menu, FIRST_CONTENT + 1));
    }

    @Test
    void theFilterAndTheSearchNarrowTheListTogether() {
        WordsMenu menu = menu("alfa", "alamo", "beta", "avion");
        UiTesting.select(menu, 1);

        UiTesting.submit(menu, "al");

        assertEquals("alfa", nameAt(menu, FIRST_CONTENT));
        assertEquals("alamo", nameAt(menu, FIRST_CONTENT + 1));
        assertNull(UiTesting.itemAt(menu, FIRST_CONTENT + 2));
    }

    @Test
    void choosingAFilterGoesBackToTheFirstPage() {
        String[] many = IntStream.range(0, 40).mapToObj(number -> "a" + number).toArray(String[]::new);
        WordsMenu menu = menu(many);
        UiTesting.click(menu, 50);

        UiTesting.select(menu, 1);

        assertEquals("a0", nameAt(menu, FIRST_CONTENT));
    }

    @Test
    void aFilterThatKeepsNothingShowsTheEmptyMarker() {
        WordsMenu menu = menu("beta");

        UiTesting.select(menu, 1);

        assertEquals(Items.PAPER, UiTesting.itemAt(menu, 22).stack().getItem());
    }

    @Test
    void aClickOnTheChestDropdownMovesToTheNextFilter() {
        WordsMenu menu = menu("alfa", "beta");

        UiTesting.click(menu, FILTER_SLOT);

        assertEquals("alfa", nameAt(menu, FIRST_CONTENT));
        assertNull(UiTesting.itemAt(menu, FIRST_CONTENT + 1));
    }

    @Test
    void onlyAsManyFiltersAsTheDropdownCanHoldAreOffered() {
        WordsMenu menu = menu("alfa");
        menu.filters.addAll(Collections.nCopies(60, new UiPagedMenu.Filter<>(Component.literal("x"), word -> true)));

        menu.refresh();

        List<UiElement> controls = ((UiElement.Row) ((UiElement.Column) UiTesting.root(menu)).children().getLast()).children();
        UiElement.Dropdown dropdown = (UiElement.Dropdown) controls.get(2);
        assertEquals(UiElement.Dropdown.MAX_OPTIONS, dropdown.options().size());
    }
}
