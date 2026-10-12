package com.panita.enriquecraft.link.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;
import com.panita.enriquecraft.link.service.LinkRunner;
import com.panita.enriquecraft.link.service.LinkSync;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.Optional;

/**
 * The moments the whitelist is synced: when the server has started, when staff reload the mod or the whitelist, and
 * when a player leaves while another's change waits for them. There is no timer: staff decide when to look.
 */
public final class LinkListener implements ModListener {

    private final LinkRunner runner;
    private final LinkSync sync;
    private final ReloadHooks reloads;

    public LinkListener(LinkRunner runner, LinkSync sync, ReloadHooks reloads) {
        this.runner = runner;
        this.sync = sync;
        this.reloads = reloads;
    }

    @Override
    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runner.run(server, Optional.empty()));
        AuthEvents.WHITELIST_RELOADED.register(server -> runner.run(server, Optional.empty()));
        reloads.onReload(source -> runner.run(source.getServer(), Optional.of(source)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (sync.hasWaiting()) {
                runner.run(server, Optional.empty());
            }
        });
    }
}
