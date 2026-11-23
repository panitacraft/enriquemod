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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

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

    /** The prank levels the player may choose, in the order they are listed. */
    List<EmblemView> choosableLevels(ServerPlayer player) {
        return profiles.choosablePranks(player);
    }

    /** The id of the prank level the player shows now. */
    String prankOf(ServerPlayer player) {
        return profiles.emblemsOf(player).prank().id();
    }

    /** How long the player must wait to change their prank level; empty when they may now, as holders of the bypass always may. */
    Optional<Duration> prankWait(ServerPlayer player) {
        return ProfilePermissions.canBypassPrankCooldown(player) ? Optional.empty() : profiles.prankWait(player);
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

    /** A change that is not one says nothing: a form sends every field, the ones that did not change too. */
    void bio(ServerPlayer player, String text) {
        if (details.detailsOf(player).bio().equals(text)) {
            return;
        }
        view.saved(player, details.setBio(player, text), Message.success(ProfileMessages.Edit.BIO_SAVED));
    }

    void pronouns(ServerPlayer player, String text) {
        if (details.detailsOf(player).pronouns().equals(text)) {
            return;
        }
        view.saved(player, details.setPronouns(player, text), Message.success(ProfileMessages.Edit.PRONOUNS_SAVED));
    }

    void markBase(ServerPlayer player) {
        ProfileDetailsService.Outcome outcome = details.markBase(player);
        view.saved(player, outcome, ProfileView.baseMarked(CoordinateView.position(player.getX(), player.getY(), player.getZ())));
    }

    void clearBase(ServerPlayer player) {
        view.saved(player, details.clearBase(player), Message.success(ProfileMessages.Edit.BASE_CLEARED));
    }
}
