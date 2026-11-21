package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.dialog.DialogService;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Forgets a player's open dialog when they leave: it would never be answered. */
public final class DialogListener implements ModListener {

    private final DialogService dialogs;

    public DialogListener(DialogService dialogs) {
        this.dialogs = dialogs;
    }

    @Override
    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> dialogs.forget(handler.getPlayer().getUUID()));
    }
}
