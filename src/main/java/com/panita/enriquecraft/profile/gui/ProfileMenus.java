package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.dialog.DialogService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.LocatorBarService;
import com.panita.enriquecraft.profile.service.ProfileDetailsService;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import com.panita.enriquecraft.profile.service.ProfileSubjects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Opens the profile menu and the menus it leads to. It holds what those menus need, so the commands and the client's key can open the
 * menus without knowing about any of them.
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
    private final ProfileSubjects subjects;
    private final ProfileEdits edits;
    private final ProfileDialogs dialogs;

    public ProfileMenus(UiService ui, ProfileService profiles, ProfileRefresher refresher, ProfileView view, EmblemTooltip tooltip,
                        ProfileConfig config, Messenger messenger, OpenMenus openMenus, ProfileSubjects subjects,
                        ProfileDetailsService details, LocatorBarService locatorBar, DialogService dialogService) {
        this.ui = ui;
        this.profiles = profiles;
        this.refresher = refresher;
        this.view = view;
        this.tooltip = tooltip;
        this.config = config;
        this.messenger = messenger;
        this.openMenus = openMenus;
        this.subjects = subjects;
        this.edits = new ProfileEdits(profiles, details, locatorBar, view);
        this.dialogs = new ProfileDialogs(dialogService, tooltip, edits, details, refresher, profiles, config, view, ui.factory());
    }

    /**
     * Opens the player's own profile with what is known now. When that is older than the cache allows, the player's data is read again
     * in the background, which redraws the menu when it arrives.
     */
    public void open(ServerPlayer player) {
        new ProfileMenu(this, Optional.empty(), false).open(player);
        refresher.refresh(player);
    }

    /** Opens the player's own profile ready to be edited. */
    public void edit(ServerPlayer player) {
        new ProfileMenu(this, Optional.empty(), true).open(player);
        refresher.refresh(player);
    }

    /** Opens the profile of the player with that name, or the viewer's own when it is their name. */
    public void openOf(ServerPlayer viewer, String name) {
        if (viewer.getGameProfile().name().equalsIgnoreCase(name)) {
            open(viewer);
        } else {
            new ProfileMenu(this, Optional.of(name), false).open(viewer);
        }
    }

    UiMenu emblemsMenu(UiMenu profileMenu, int slot) {
        return new EmblemsMenu(ui, profiles, tooltip, config, openMenus, profileMenu, slot);
    }

    UiMenu pranksMenu(UiMenu profileMenu) {
        return new PranksMenu(ui, edits, tooltip, profileMenu);
    }

    /** The rank the player shows in chat, which is empty for a player with none. */
    Component rankOf(ServerPlayer player) {
        return messenger.format(Message.plain("%enriquecraft:rank%"), player);
    }

    UiMenu emblemDetail(UiMenu profileMenu, EmblemView emblem) {
        return new EmblemDetailMenu(ui, tooltip, profileMenu, emblem);
    }

    UiService ui() {
        return ui;
    }

    ProfileSubjects subjects() {
        return subjects;
    }

    EmblemTooltip tooltip() {
        return tooltip;
    }

    OpenMenus openMenus() {
        return openMenus;
    }

    ProfileEdits edits() {
        return edits;
    }

    ProfileDialogs dialogs() {
        return dialogs;
    }

    ProfileRefresher refresher() {
        return refresher;
    }

    ProfileView view() {
        return view;
    }
}
