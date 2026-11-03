package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.profile.service.ProfileLoader;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Tells players and staff how a refresh of profiles went. Presentation only: what a refresh does is decided by
 * {@link ProfileRefresher}.
 */
public final class ProfileView {

    private final Messenger messenger;

    public ProfileView(Messenger messenger) {
        this.messenger = messenger;
    }

    /** How a player's own refresh went. */
    public void selfRefresh(ServerPlayer player, ProfileRefresher.SelfResult result) {
        switch (result) {
            case ProfileRefresher.SelfResult.Cooling cooling ->
                    messenger.send(player, Message.warning(ProfileMessages.Refresh.COOLDOWN).prefixed().with("seconds", (int) cooling.seconds()));
            case ProfileRefresher.SelfResult.Finished finished -> messenger.send(player, switch (finished.outcome()) {
                case ProfileLoader.Outcome.Updated updated -> Message.success(ProfileMessages.Refresh.UPDATED).prefixed();
                case ProfileLoader.Outcome.NotLinked notLinked -> Message.warning(ProfileMessages.Refresh.NOT_LINKED).prefixed();
                case ProfileLoader.Outcome.Failed failed -> Message.error(ProfileMessages.Refresh.FAILED).prefixed();
                case ProfileLoader.Outcome.Busy busy -> Message.info(ProfileMessages.Refresh.BUSY).prefixed();
            });
        }
    }

    /** How the refresh of everyone went; empty when one was already running. */
    public void allRefreshed(CommandSourceStack source, Optional<ProfileRefresher.Report> outcome) {
        outcome.ifPresentOrElse(
                report -> messenger.send(source, Message.success(ProfileMessages.Refresh.ALL_DONE).prefixed()
                        .with("updated", report.updated()).with("notLinked", report.notLinked()).with("failed", report.failed())),
                () -> messenger.send(source, Message.warning(ProfileMessages.Refresh.ALL_BUSY).prefixed()));
    }
}
