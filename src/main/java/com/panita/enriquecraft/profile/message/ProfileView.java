package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.core.event.CoreEvents;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileLoader;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
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
                // A forced read never ends as current; it is listed because the outcome type is shared with reads that may.
                case ProfileLoader.Outcome.Current current -> Message.success(ProfileMessages.Refresh.UPDATED).prefixed();
                case ProfileLoader.Outcome.NotLinked notLinked -> Message.warning(ProfileMessages.Refresh.NOT_LINKED).prefixed();
                case ProfileLoader.Outcome.Failed failed -> Message.error(ProfileMessages.Refresh.FAILED).prefixed();
                case ProfileLoader.Outcome.Busy busy -> Message.info(ProfileMessages.Refresh.BUSY).prefixed();
            });
        }
    }

    /**
     * Tells everyone that a player changed their prank level, in the game and, through {@link CoreEvents#BROADCAST}, in Discord. The
     * game's line carries the mod prefix; the one for Discord is the same words without it.
     */
    public void prankChanged(ServerPlayer player, String level) {
        MinecraftServer server = player.level().getServer();
        Message announcement = announcement(player.getDisplayName(), level);
        messenger.deliver(server, server.getPlayerList().getPlayers(), messenger.format(announcement.prefixed()));
        CoreEvents.BROADCAST.invoker().onBroadcast(plainAnnouncement(player.getDisplayName(), level));
    }

    /**
     * What asking to change the prank level came to, for the menu and for {@code /prank set}.
     *
     * @param level the title of the level asked for, or what was typed when it is not one
     */
    public void prankResult(ServerPlayer player, String level, ProfileService.PrankChange change) {
        switch (change.status()) {
            case CHANGED -> prankChanged(player, level);
            case COOLDOWN -> messenger.send(player, Message.warning(ProfileMessages.Prank.COOLDOWN).prefixed()
                    .with("time", Durations.format(change.remaining())));
            case NOT_LINKED -> messenger.send(player, Message.warning(ProfileMessages.Prank.NOT_LINKED).prefixed());
            case SAME -> messenger.send(player, Message.info(ProfileMessages.Prank.SAME).prefixed().with("level", level));
            case LOCKED -> messenger.send(player, Message.warning(ProfileMessages.Prank.LOCKED).prefixed());
            case UNKNOWN -> messenger.send(player, Message.error(ProfileMessages.Prank.UNKNOWN).prefixed().with("level", level));
        }
    }

    /** The level a player is at and the ones they may choose, each a click away from the command that chooses it. */
    public void prankList(ServerPlayer player, PlayerEmblems emblems) {
        messenger.send(player, Message.info(ProfileMessages.Prank.LIST_CURRENT).prefixed().with("level", emblems.prank().title()));
        List<ServerPlayer> reader = List.of(player);
        messenger.deliver(reader, messenger.format(Message.plain(ProfileMessages.Prank.LIST_HEADER)));
        HoverEvent hover = new HoverEvent.ShowText(messenger.format(Message.plain(ProfileMessages.Prank.LIST_HOVER)));
        // The first level is only where players start, so it is not one to choose
        emblems.pranks().stream().skip(1).forEach(level -> messenger.deliver(reader,
                messenger.format(Message.plain(ProfileMessages.Prank.LIST_ENTRY).with("title", level.title()).with("id", level.id())).copy()
                        .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/prank set " + level.id())).withHoverEvent(hover))));
    }

    private static Message announcement(Component player, String level) {
        return Message.plain(ProfileMessages.Prank.CHANGED).with("player", player).with("level", level);
    }

    /** What Discord is told: the announcement as plain words, with no prefix and no colors. */
    String plainAnnouncement(Component player, String level) {
        return messenger.format(announcement(player, level)).getString();
    }

    /** How the refresh of everyone went; empty when one was already running. */
    public void allRefreshed(CommandSourceStack source, Optional<ProfileRefresher.Report> outcome) {
        outcome.ifPresentOrElse(
                report -> messenger.send(source, Message.success(ProfileMessages.Refresh.ALL_DONE).prefixed()
                        .with("updated", report.updated()).with("notLinked", report.notLinked()).with("failed", report.failed())),
                () -> messenger.send(source, Message.warning(ProfileMessages.Refresh.ALL_BUSY).prefixed()));
    }
}
