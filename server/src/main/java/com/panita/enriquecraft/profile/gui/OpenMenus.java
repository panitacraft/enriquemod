package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.ui.UiMenu;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The profile menu each player is looking at, so it can be drawn again when their data changes while it is open. A menu notes itself
 * when it is drawn and lets go when it closes, so only the one on screen is ever redrawn.
 */
public final class OpenMenus {

    private final Map<UUID, UiMenu> current = new ConcurrentHashMap<>();

    void seen(UUID player, UiMenu menu) {
        current.put(player, menu);
    }

    void closed(UUID player, UiMenu menu) {
        current.remove(player, menu);
    }

    /** Draws the player's menu again, if they have one open. Call it on the server thread. */
    public void refresh(UUID player) {
        UiMenu menu = current.get(player);
        if (menu != null) {
            menu.refresh();
        }
    }
}
