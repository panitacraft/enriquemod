package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;
import com.panita.enriquecraft.discord.service.BridgeConnection;
import com.panita.enriquecraft.discord.service.ServerInfo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Connects to the bot when the server has started and disconnects when it stops. A reload connects too if the line is not
 * up, so an address or key put in place later is picked up without a restart.
 */
public final class DiscordLifecycleListener implements ModListener {

    private final BridgeConnection connection;
    private final ServerInfo info;
    private final ReloadHooks reloads;

    public DiscordLifecycleListener(BridgeConnection connection, ServerInfo info, ReloadHooks reloads) {
        this.connection = connection;
        this.info = info;
        this.reloads = reloads;
    }

    @Override
    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            info.started(server);
            connection.start();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            connection.stop();
            info.stopped();
        });
        reloads.onReload(source -> connection.start());
    }
}
