package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.chat.service.NameTags;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Keeps the names above the heads up to date. */
public final class NameTagsListener implements ModListener {

    private final NameTags nameTags;

    public NameTagsListener(NameTags nameTags) {
        this.nameTags = nameTags;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(nameTags::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> nameTags.introduce(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> nameTags.forget(server, handler.getPlayer().getUUID()));
    }
}
