package com.panita.enriquecraft.auth.listeners;

import com.panita.enriquecraft.auth.service.PinChanges;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Keeps the screens of players who are changing their PIN moving, and drops a change when its player leaves.
 */
public final class PinChangesListener implements ModListener {

    private final PinChanges changes;

    public PinChangesListener(PinChanges changes) {
        this.changes = changes;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> changes.tick());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> changes.cancel(handler.getPlayer().getUUID()));
    }
}
