package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import com.panita.enriquecraft.profile.service.RefreshCooldown;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the profile menu and the menus it leads to. It holds what those menus need, so the command and the client's key can open the
 * menu without knowing about any of them.
 */
public final class ProfileMenus {

    private final UiService ui;
    private final ProfileService profiles;
    private final ProfileRefresher refresher;
    private final ProfileView view;
    private final EmblemTooltip tooltip;
    private final ProfileConfig config;
    private final Messenger messenger;
    private final OpenMenus openMenus;
    /** Keeps opening the menu over and over from asking the API each time. */
    private final RefreshCooldown openThrottle;

    public ProfileMenus(UiService ui, ProfileService profiles, ProfileRefresher refresher, ProfileView view, EmblemTooltip tooltip,
                        ProfileConfig config, Messenger messenger, OpenMenus openMenus, RefreshCooldown openThrottle) {
        this.ui = ui;
        this.profiles = profiles;
        this.refresher = refresher;
        this.view = view;
        this.tooltip = tooltip;
        this.config = config;
        this.messenger = messenger;
        this.openMenus = openMenus;
        this.openThrottle = openThrottle;
    }

    /**
     * Opens the profile menu with what is known now, and reads the player's data again in the background, which redraws the menu
     * when it arrives.
     */
    public void open(ServerPlayer player) {
        new ProfileMenu(ui, this, profiles, view, tooltip, refresher, openMenus).open(player);
        if (openThrottle.remainingSeconds(player.getUUID()) == 0) {
            openThrottle.start(player.getUUID());
            refresher.refresh(player);
        }
    }

    UiMenu emblemsMenu(UiMenu profileMenu) {
        return new EmblemsMenu(ui, profiles, tooltip, config, messenger, openMenus, profileMenu);
    }
}
