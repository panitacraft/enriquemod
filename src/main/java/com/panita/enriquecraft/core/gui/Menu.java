package com.panita.enriquecraft.core.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A chest-style screen shown to one player. It is purely server-side: vanilla clients see an
 * ordinary chest whose items cannot be moved.
 * <p>
 * A subclass describes the screen in {@link #draw()} using {@link #set(int, MenuItem)} and
 * {@link #frame()}. Call {@link #refresh()} after changing what the screen should show. Menus
 * keep the state of one viewing (for example the current page), so create a new menu for every
 * player and every opening.
 */
public abstract class Menu {

    private static final Logger LOGGER = LoggerFactory.getLogger(Menu.class);

    private final MenuFactory factory;
    private final Menu previous;
    private MenuSlots<MenuItem> slots;
    private SimpleContainer container;
    private MenuScreen screen;

    /**
     * @param factory creates the texts and items of the screen
     * @param previous the menu a back button returns to, or null
     */
    protected Menu(MenuFactory factory, Menu previous) {
        this.factory = factory;
        this.previous = previous;
    }

    protected abstract Component title();

    /** The height of the screen in rows, from 1 to 6. */
    protected abstract int rows();

    /** Fills the screen with {@link #set(int, MenuItem)}. Called on open and on every refresh. */
    protected abstract void draw();

    /** Called when the player closes the screen or another one replaces it. */
    protected void onClose() {
    }

    public final void open(ServerPlayer player) {
        prepare();
        player.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) -> {
            screen = new MenuScreen(containerId, inventory, container, rows(), this);
            return screen;
        }, title()));
    }

    /** Builds the empty screen and draws it; the first step of opening, separate so tests can run it. */
    final void prepare() {
        int size = rows() * MenuFrame.COLUMNS;
        slots = new MenuSlots<>(size);
        container = new SimpleContainer(size);
        draw();
    }

    /** What a slot currently shows, or null when it is empty. */
    final MenuItem itemAt(int slot) {
        return slots.get(slot);
    }

    /** Redraws the screen and sends the changes to the viewer. */
    public final void refresh() {
        slots.clear();
        container.clearContent();
        draw();
        if (screen != null) {
            screen.broadcastFullState();
        }
    }

    protected final void set(int slot, MenuItem item) {
        slots.set(slot, item);
        container.setItem(slot, item.stack());
    }

    /** Fills the outer edge of the screen with the decorative filler. Call it first in {@link #draw()}. */
    protected final void frame() {
        MenuItem filler = MenuItem.display(factory.filler());
        for (int slot : MenuFrame.borderSlots(rows())) {
            set(slot, filler);
        }
    }

    /** Fills every slot that is still empty with the decorative filler. Call it last in {@link #draw()}. */
    protected final void fillRest() {
        MenuItem filler = MenuItem.display(factory.filler());
        for (int slot = 0; slot < rows() * MenuFrame.COLUMNS; slot++) {
            if (slots.get(slot) == null) {
                set(slot, filler);
            }
        }
    }

    protected final MenuFactory factory() {
        return factory;
    }

    /** The menu a back button returns to, or null when this menu was opened directly. */
    protected final Menu previous() {
        return previous;
    }

    final void handleClick(MenuClick click) {
        // A number key or a drop over a button must not press it.
        MenuItem item = click.isPlain() ? slots.get(click.slot()) : null;
        if (item == null) {
            return;
        }
        try {
            item.action().accept(click);
        } catch (RuntimeException e) {
            LOGGER.error("A menu action failed for {}", click.player().getName().getString(), e);
        }
    }

    final void closed() {
        screen = null;
        onClose();
    }
}
