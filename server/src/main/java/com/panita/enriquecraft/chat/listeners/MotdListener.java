package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.chat.service.MotdService;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;

/**
 * Shows a changed server list text as soon as staff run {@code /enriquecraft reload}, without a restart.
 */
public final class MotdListener implements ModListener {

    private final MotdService motd;
    private final ReloadHooks reloads;

    public MotdListener(MotdService motd, ReloadHooks reloads) {
        this.motd = motd;
        this.reloads = reloads;
    }

    @Override
    public void register() {
        reloads.onReload(source -> motd.refresh(source.getServer()));
    }
}
