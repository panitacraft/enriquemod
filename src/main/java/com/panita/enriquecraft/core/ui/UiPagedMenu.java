package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.Paginator;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
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
        Optional<Function<T, String>> text = searchText();
        if (text.isEmpty() || query.isEmpty()) {
            return all;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return all.stream().filter(entry -> text.get().apply(entry).toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    /** One chest row wide, so the controls land on the bottom edge of the frame. */
    private UiElement controls(UiBuilder builder, int total) {
        UiElement.Spacer none = new UiElement.Spacer();
        Message indicator = Message.plain(Messages.Gui.PAGE_INDICATOR)
                .with("page", page + 1)
                .with("pages", PAGINATOR.pageCount(total));

        UiElement back = previous() == null ? none
                : builder.button(new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK), List.of(),
                        click -> previous().open(click.player()));
        UiElement search = searchText().isEmpty() ? none
                : builder.input(factory().text(Messages.Gui.SEARCH), query, SEARCH_LENGTH, submit -> search(submit.text()));
        UiElement previousPage = PAGINATOR.hasPrevious(page) ? builder.button(new ItemStack(Items.ARROW),
                factory().text(Messages.Gui.PREVIOUS), List.of(), click -> turn(-1)) : none;
        UiElement close = builder.button(new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE),
                List.<Component>of(factory().text(indicator)), click -> ui().close(click.player()));
        UiElement nextPage = PAGINATOR.hasNext(page, total) ? builder.button(new ItemStack(Items.ARROW),
                factory().text(Messages.Gui.NEXT), List.of(), click -> turn(1)) : none;

        return new UiElement.Row(List.of(back, search, none, previousPage, close, nextPage, none, none, none));
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
