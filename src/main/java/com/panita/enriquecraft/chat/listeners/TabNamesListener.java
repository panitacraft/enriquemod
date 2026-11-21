package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.chat.service.TabNames;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Keeps the names of the tab list up to date. */
public final class TabNamesListener implements ModListener {

    private final TabNames tabNames;

    public TabNamesListener(TabNames tabNames) {
        this.tabNames = tabNames;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(tabNames::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> tabNames.forget(handler.getPlayer().getUUID()));
    }
}
