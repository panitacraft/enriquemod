package com.panita.enriquecraft.link.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;
import com.panita.enriquecraft.link.config.LinkConfig;
import com.panita.enriquecraft.link.service.LinkRunner;
import com.panita.enriquecraft.link.service.LinkSync;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

/**
 * The moments the whitelist is synced: when the server has started, when staff reload the mod or the whitelist, and
 * when a player leaves while another's change waits for them. There is no timer: staff decide when to look. None of it
 * happens when {@code link.autoFetch} is off.
 */
public final class LinkListener implements ModListener {

    private final LinkRunner runner;
    private final LinkSync sync;
    private final ReloadHooks reloads;
    private final LinkConfig config;

    public LinkListener(LinkRunner runner, LinkSync sync, ReloadHooks reloads, LinkConfig config) {
        this.runner = runner;
        this.sync = sync;
        this.reloads = reloads;
        this.config = config;
    }

    @Override
    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runAutomatically(server));
        AuthEvents.WHITELIST_RELOADED.register(server -> runAutomatically(server));
        reloads.onReload(source -> {
            if (config.autoFetch.get()) {
                runner.run(source.getServer(), Optional.of(source));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (sync.hasWaiting()) {
                runAutomatically(server);
            }
        });
    }

    /** A sync nobody asked for: it only runs when the administrator lets the mod read the API by itself. */
    private void runAutomatically(MinecraftServer server) {
        if (config.autoFetch.get()) {
            runner.run(server, Optional.empty());
        }
    }
}
