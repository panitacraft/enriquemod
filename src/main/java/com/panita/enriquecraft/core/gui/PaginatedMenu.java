package com.panita.enriquecraft.core.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * A six-row screen that lists entries a page at a time inside a decorative frame. The bottom row
 * has previous, close (with the page indicator) and next buttons, and a back button when the menu
 * was opened from another one.
 *
 * @param <T> what one entry is
 */
public abstract class PaginatedMenu<T> extends Menu {

    private static final int ROWS = 6;
    private static final int BACK_SLOT = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int EMPTY_SLOT = 22;

    private static final List<Integer> CONTENT_SLOTS = MenuFrame.interiorSlots(ROWS);
    private static final Paginator PAGINATOR = new Paginator(CONTENT_SLOTS.size());

    private int page;

    protected PaginatedMenu(MenuFactory factory, Menu previous) {
        super(factory, previous);
    }

    /** All entries, read again on every refresh so removals show up. */
    protected abstract List<T> entries();

    protected abstract MenuItem render(T entry);

    @Override
    protected final int rows() {
        return ROWS;
    }

    @Override
    protected final void draw() {
        frame();
        List<T> entries = entries();
        page = PAGINATOR.clamp(page, entries.size());

        List<T> visible = PAGINATOR.slice(entries, page);
        for (int index = 0; index < visible.size(); index++) {
            set(CONTENT_SLOTS.get(index), render(visible.get(index)));
        }
        if (entries.isEmpty()) {
            set(EMPTY_SLOT, MenuItem.display(factory().item(Items.PAPER).name(Messages.Gui.EMPTY).build()));
        }
        drawControls(entries.size());
    }

    private void drawControls(int total) {
        MenuFactory factory = factory();
        if (PAGINATOR.hasPrevious(page)) {
            set(PREVIOUS_SLOT, MenuItem.button(factory.item(Items.ARROW).name(Messages.Gui.PREVIOUS).build(),
                    click -> turn(-1)));
        }
        if (PAGINATOR.hasNext(page, total)) {
            set(NEXT_SLOT, MenuItem.button(factory.item(Items.ARROW).name(Messages.Gui.NEXT).build(),
                    click -> turn(1)));
        }
        Message indicator = Message.plain(Messages.Gui.PAGE_INDICATOR)
                .with("page", page + 1)
                .with("pages", PAGINATOR.pageCount(total));
        set(CLOSE_SLOT, MenuItem.button(factory.item(Items.BARRIER).name(Messages.Gui.CLOSE).lore(indicator).build(),
                click -> click.player().closeContainer()));
        if (previous() != null) {
            set(BACK_SLOT, MenuItem.button(factory.item(Items.OAK_DOOR).name(Messages.Gui.BACK).build(),
                    click -> previous().open(click.player())));
        }
    }

    private void turn(int direction) {
        page += direction;
        refresh();
    }
}
