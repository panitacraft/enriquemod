package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shows {@link UiMenu}s to players, choosing the form their client can display.
 */
public final class UiService {

    private final MenuFactory factory;

    public UiService(MenuFactory factory) {
        this.factory = factory;
    }

    /** Creates the texts and items that screens are made of. */
    public MenuFactory factory() {
        return factory;
    }

    void open(ServerPlayer player, UiMenu menu) {
        UiChestMenu chest = new UiChestMenu(menu);
        menu.showingAs(chest);
        chest.open(player);
    }

    void refresh(UiMenu menu) {
        if (menu.chest() != null) {
            menu.chest().refresh();
        }
    }

    /** Closes whatever screen the player has open. */
    public void close(ServerPlayer player) {
        player.closeContainer();
    }
}
