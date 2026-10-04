package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.Menu;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A screen shown to one player, described once and shown the best way their client allows: as the
 * client companion's own screen, or as a vanilla chest. Menus keep the state of one viewing (for
 * example the current page), so create a new one for every player and every opening.
 * <p>
 * A subclass describes the screen in {@link #describe(UiBuilder)}, which is called again on every
 * {@link #refresh()}; read data there, not in the constructor.
 */
public abstract class UiMenu {

    private final UiService ui;
    private final UiMenu previous;
    private ServerPlayer viewer;
    private Menu chest;
    private boolean open;

    /**
     * @param ui       shows the screen and creates its texts and items
     * @param previous the menu a back button returns to, or null
     */
    protected UiMenu(UiService ui, UiMenu previous) {
        this.ui = ui;
        this.previous = previous;
    }

    protected abstract Component title();

    /** The screen as it is right now. */
    protected abstract UiElement describe(UiBuilder builder);

    /** An item shown beside the title by the client companion; none by default. A chest has no place for it. */
    protected ItemStack icon() {
        return ItemStack.EMPTY;
    }

    /** How a chest decorates the screen; see {@link ChestLayout}. */
    protected ChestStyle chestStyle() {
        return ChestStyle.PLAIN;
    }

    /** Called when the player closes the screen or another one replaces it. */
    protected void onClose() {
    }

    public final void open(ServerPlayer player) {
        viewer = player;
        opened();
        ui.open(player, this);
    }

    /** Redraws the screen and sends the changes to the viewer. */
    public final void refresh() {
        ui.refresh(this);
    }

    protected final UiService ui() {
        return ui;
    }

    protected final MenuFactory factory() {
        return ui.factory();
    }

    /** The menu a back button returns to, or null when this menu was opened directly. */
    protected final UiMenu previous() {
        return previous;
    }

    final UiLayout layout(int firstId) {
        UiBuilder builder = new UiBuilder(firstId);
        return builder.build(describe(builder));
    }

    final ServerPlayer viewer() {
        return viewer;
    }

    final Menu chest() {
        return chest;
    }

    final void showingAs(Menu chest) {
        this.chest = chest;
    }

    /** Marks the menu as being shown, so that closing it reaches {@link #onClose()}. */
    final void opened() {
        open = true;
    }

    final void closed() {
        // The same menu can be closed by several paths at once, such as the player and a disconnect.
        if (open) {
            open = false;
            chest = null;
            onClose();
        }
    }
}
