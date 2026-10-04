package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.Paginator;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A screen that lists entries a page at a time in a grid. Below the grid are previous, close (with
 * the page indicator) and next buttons, a back button when the menu was opened from another one,
 * and a search box when the menu offers one. As a chest it is six rows inside a decorative frame.
 *
 * @param <T> what one entry is
 */
public abstract class UiPagedMenu<T> extends UiMenu {

    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 4;
    // The cell where the "nothing to show" marker goes: the middle of the grid.
    private static final int EMPTY_CELL = 10;
    private static final int SEARCH_LENGTH = 32;

    private static final Paginator PAGINATOR = new Paginator(GRID_COLUMNS * GRID_ROWS);

    private int page;
    private String query = "";
    private int filterIndex;

    /** One way to narrow the list: what it is called in the dropdown, and which entries it keeps. */
    public record Filter<T>(Component name, Predicate<T> keeps) {
    }

    protected UiPagedMenu(UiService ui, UiMenu previous) {
        super(ui, previous);
    }

    /** All entries, read again on every refresh so removals show up. */
    protected abstract List<T> entries();

    protected abstract UiElement render(UiBuilder builder, T entry);

    /**
     * The text of an entry that the search box looks in, ignoring case. Return a function to give the
     * menu a search box; the default is no search box.
     */
    protected Optional<Function<T, String>> searchText() {
        return Optional.empty();
    }

    /**
     * The ways the list can be narrowed, offered in a dropdown next to the search box together with "no
     * filter". Read again on every refresh, so the options can follow the data; at most
     * {@link UiElement.Dropdown#MAX_OPTIONS} minus one are shown. None by default.
     */
    protected List<Filter<T>> filters() {
        return List.of();
    }

    @Override
    protected final ChestStyle chestStyle() {
        return ChestStyle.FRAMED;
    }

    @Override
    protected final UiElement describe(UiBuilder builder) {
        List<T> entries = matching(entries());
        page = PAGINATOR.clamp(page, entries.size());

        List<UiElement> cells = new ArrayList<>();
        PAGINATOR.slice(entries, page).forEach(entry -> cells.add(render(builder, entry)));
        if (entries.isEmpty()) {
            cells.addAll(Collections.nCopies(EMPTY_CELL, new UiElement.Spacer()));
            cells.add(new UiElement.Label(factory().text(Messages.Gui.EMPTY)));
        }
        return new UiElement.Column(List.of(
                new UiElement.Grid(GRID_COLUMNS, GRID_ROWS, cells),
                controls(builder, entries.size())));
    }

    private List<T> matching(List<T> all) {
        List<T> kept = all;
        Optional<Function<T, String>> text = searchText();
        if (text.isPresent() && !query.isEmpty()) {
            String needle = query.toLowerCase(Locale.ROOT);
            kept = kept.stream().filter(entry -> text.get().apply(entry).toLowerCase(Locale.ROOT).contains(needle)).toList();
        }
        List<Filter<T>> filters = shownFilters();
        filterIndex = Math.min(filterIndex, filters.size());
        if (filterIndex > 0) {
            Predicate<T> keeps = filters.get(filterIndex - 1).keeps();
            kept = kept.stream().filter(keeps).toList();
        }
        return kept;
    }

    private List<Filter<T>> shownFilters() {
        List<Filter<T>> filters = filters();
        return filters.subList(0, Math.min(filters.size(), UiElement.Dropdown.MAX_OPTIONS - 1));
    }

    /** One chest row wide, so the controls land on the bottom edge of the frame. */
    private UiElement controls(UiBuilder builder, int total) {
        UiElement.Spacer none = new UiElement.Spacer();
        Message indicator = Message.plain(Messages.Gui.PAGE_INDICATOR)
                .with("page", page + 1)
                .with("pages", PAGINATOR.pageCount(total));

        UiElement back = previous() == null ? none
                : builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK), List.of(),
                        click -> previous().open(click.player()));
        UiElement search = searchText().isEmpty() ? none
                : builder.input(factory().text(Messages.Gui.SEARCH), query, SEARCH_LENGTH, submit -> search(submit.text()));
        UiElement filter = filterDropdown(builder, none);
        UiElement previousPage = PAGINATOR.hasPrevious(page) ? builder.button(ButtonRole.PREVIOUS, new ItemStack(Items.ARROW),
                factory().text(Messages.Gui.PREVIOUS), List.of(), click -> turn(-1)) : none;
        UiElement close = builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE),
                List.<Component>of(factory().text(indicator)), click -> ui().close(click.player()));
        UiElement nextPage = PAGINATOR.hasNext(page, total) ? builder.button(ButtonRole.NEXT, new ItemStack(Items.ARROW),
                factory().text(Messages.Gui.NEXT), List.of(), click -> turn(1)) : none;

        // The page indicator sits in a slot that is empty in a chest, so the chest looks as it always did.
        UiElement pageIndicator = new UiElement.Page(page + 1, PAGINATOR.pageCount(total));
        return new UiElement.Row(List.of(back, search, filter, previousPage, close, nextPage, pageIndicator, none, none));
    }

    /** The dropdown that narrows the list, or a spacer when the menu has no filters. */
    private UiElement filterDropdown(UiBuilder builder, UiElement none) {
        List<Filter<T>> filters = shownFilters();
        if (filters.isEmpty()) {
            return none;
        }
        List<Component> options = new ArrayList<>();
        options.add(factory().text(Messages.Gui.FILTER_NONE));
        filters.forEach(filter -> options.add(filter.name()));
        return builder.dropdown(factory().text(Messages.Gui.FILTER), options, filterIndex, select -> {
            filterIndex = select.option();
            page = 0;
            refresh();
        });
    }

    private void search(String text) {
        query = text.trim();
        page = 0;
        refresh();
    }

    private void turn(int direction) {
        page += direction;
        refresh();
    }
}
