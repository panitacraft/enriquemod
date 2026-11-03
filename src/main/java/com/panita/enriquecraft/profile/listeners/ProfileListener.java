package com.panita.enriquecraft.profile.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import com.panita.enriquecraft.profile.service.RefreshCooldown;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * The moments a player's profile is read: when they are ready to play, and when staff reload the mod. It is dropped when they
 * leave. There is no timer: a change made on the website shows at the next of these, or when somebody refreshes.
 */
public final class ProfileListener implements ModListener {

    private final ProfileRefresher refresher;
    private final ProfileService profiles;
    private final RefreshCooldown cooldown;
    private final ProfileView view;
    private final ReloadHooks reloads;

    public ProfileListener(ProfileRefresher refresher, ProfileService profiles, RefreshCooldown cooldown, ProfileView view,
                           ReloadHooks reloads) {
        this.refresher = refresher;
        this.profiles = profiles;
        this.cooldown = cooldown;
        this.view = view;
        this.reloads = reloads;
    }

    @Override
    public void register() {
        AuthEvents.PLAYER_READY.register(refresher::refresh);
        reloads.onReload(source -> refresher.refreshAll(source.getServer()).thenAccept(report -> view.allRefreshed(source, report)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            profiles.forget(handler.getPlayer().getUUID());
            cooldown.forget(handler.getPlayer().getUUID());
        });
    }
}
