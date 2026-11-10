package com.panita.enriquecraft.profile.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.RefreshCooldown;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * The moments a player's profile is read: when they are ready to play (only if the cache is older than {@code profile.cacheSeconds}),
 * and always when staff reload the mod. There is no timer: a change made on the website shows at the next of these, or when
 * somebody refreshes.
 */
public final class ProfileListener implements ModListener {

    private final ProfileRefresher refresher;
    private final RefreshCooldown cooldown;
    private final ProfileView view;
    private final ReloadHooks reloads;

    public ProfileListener(ProfileRefresher refresher, RefreshCooldown cooldown, ProfileView view, ReloadHooks reloads) {
        this.refresher = refresher;
        this.cooldown = cooldown;
        this.view = view;
        this.reloads = reloads;
    }

    @Override
    public void register() {
        AuthEvents.PLAYER_READY.register(refresher::refresh);
        reloads.onReload(source -> refresher.refreshAll(source.getServer()).thenAccept(report -> view.allRefreshed(source, report)));
        // The profile stays cached, so coming back soon does not ask the API again.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cooldown.forget(handler.getPlayer().getUUID()));
    }
}
