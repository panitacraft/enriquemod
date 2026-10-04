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

/**
 * A screen that lists entries a page at a time in a grid. Below the grid are previous, close (with
 * the page indicator) and next buttons, and a back button when the menu was opened from another one.
 * As a chest it is six rows inside a decorative frame.
 *
 * @param <T> what one entry is
 */
public abstract class UiPagedMenu<T> extends UiMenu {

    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 4;
    // The cell where the "nothing to show" marker goes: the middle of the grid.
    private static final int EMPTY_CELL = 10;

    private static final Paginator PAGINATOR = new Paginator(GRID_COLUMNS * GRID_ROWS);

    private int page;

    protected UiPagedMenu(UiService ui, UiMenu previous) {
        super(ui, previous);
    }

    /** All entries, read again on every refresh so removals show up. */
    protected abstract List<T> entries();

    protected abstract UiElement render(UiBuilder builder, T entry);

    @Override
    protected final ChestStyle chestStyle() {
        return ChestStyle.FRAMED;
    }

    @Override
    protected final UiElement describe(UiBuilder builder) {
        List<T> entries = entries();
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

    /** One chest row wide, so the controls land on the bottom edge of the frame. */
    private UiElement controls(UiBuilder builder, int total) {
        UiElement.Spacer none = new UiElement.Spacer();
        Message indicator = Message.plain(Messages.Gui.PAGE_INDICATOR)
                .with("page", page + 1)
                .with("pages", PAGINATOR.pageCount(total));

        UiElement back = previous() == null ? none
                : builder.button(new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK), List.of(),
                        click -> previous().open(click.player()));
        UiElement previousPage = PAGINATOR.hasPrevious(page) ? builder.button(new ItemStack(Items.ARROW),
                factory().text(Messages.Gui.PREVIOUS), List.of(), click -> turn(-1)) : none;
        UiElement close = builder.button(new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE),
                List.<Component>of(factory().text(indicator)), click -> ui().close(click.player()));
        UiElement nextPage = PAGINATOR.hasNext(page, total) ? builder.button(new ItemStack(Items.ARROW),
                factory().text(Messages.Gui.NEXT), List.of(), click -> turn(1)) : none;

        return new UiElement.Row(List.of(back, none, none, previousPage, close, nextPage, none, none, none));
    }

    private void turn(int direction) {
        page += direction;
        refresh();
    }
}
