package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.profile.ProfilePermissions;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.LocatorBarService;
import com.panita.enriquecraft.profile.service.ProfileDetailsService;
import com.panita.enriquecraft.profile.service.ProfileService;
import com.panita.enriquecraft.staff.message.CoordinateView;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * What a player changes in the edit view, whether they do it in the client companion's screen or in a dialog: each change is saved,
 * applied where it has an effect in the game, and told to the player. The two forms only differ in how they ask.
 */
final class ProfileEdits {

    private final ProfileService profiles;
    private final ProfileDetailsService details;
    private final LocatorBarService locatorBar;
    private final ProfileView view;

    ProfileEdits(ProfileService profiles, ProfileDetailsService details, LocatorBarService locatorBar, ProfileView view) {
        this.profiles = profiles;
        this.details = details;
        this.locatorBar = locatorBar;
        this.view = view;
    }

    /** The prank levels in the order a dropdown lists them. */
    List<EmblemView> levels() {
        return profiles.prankLevels();
    }

    /** The id of the prank level the player shows now. */
    String prankOf(ServerPlayer player) {
        return profiles.emblemsOf(player).prank().id();
    }

    void prank(ServerPlayer player, String levelId) {
        String title = levels().stream().filter(level -> level.id().equals(levelId)).map(EmblemView::title).findFirst().orElse(levelId);
        view.prankResult(player, title, profiles.choosePrank(player, levelId, ProfilePermissions.canBypassPrankCooldown(player)));
    }

    void locator(ServerPlayer player, boolean shared) {
        ProfileDetailsService.Outcome outcome = details.setShareLocation(player, shared);
        if (outcome == ProfileDetailsService.Outcome.SAVED) {
            locatorBar.apply(player, shared);
        }
        view.saved(player, outcome, ProfileView.locatorChanged(shared));
    }

    void bio(ServerPlayer player, String text) {
        view.saved(player, details.setBio(player, text), Message.success(ProfileMessages.Edit.BIO_SAVED));
    }

    void markBase(ServerPlayer player) {
        ProfileDetailsService.Outcome outcome = details.markBase(player);
        view.saved(player, outcome, ProfileView.baseMarked(CoordinateView.position(player.getX(), player.getY(), player.getZ())));
    }

    void clearBase(ServerPlayer player) {
        view.saved(player, details.clearBase(player), Message.success(ProfileMessages.Edit.BASE_CLEARED));
    }
}
