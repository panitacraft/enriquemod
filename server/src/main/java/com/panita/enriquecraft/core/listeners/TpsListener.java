package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.service.TpsMonitor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Feeds every finished tick of the server to the monitor that {@code /tps} reads.
 */
public final class TpsListener implements ModListener {

    private final TpsMonitor monitor;

    public TpsListener(TpsMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> monitor.tick(server.getPlayerCount() == 0));
    }
}
