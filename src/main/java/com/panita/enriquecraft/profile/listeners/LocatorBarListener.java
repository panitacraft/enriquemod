package com.panita.enriquecraft.profile.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.profile.service.LocatorBarService;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

/** Applies each player's locator bar choice when they are let in and after a respawn, which creates a new player without it. */
public final class LocatorBarListener implements ModListener {

    private final LocatorBarService locatorBar;

    public LocatorBarListener(LocatorBarService locatorBar) {
        this.locatorBar = locatorBar;
    }

    @Override
    public void register() {
        AuthEvents.PLAYER_READY.register(locatorBar::refresh);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> locatorBar.refresh(newPlayer));
    }
}
