package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.moderation.data.MuteRecord;
import com.panita.enriquecraft.moderation.service.PunishmentTerms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.Instant;

/**
 * What a mute shows: the notice to the muted player, when they are muted and each time they try to write, and the reports to
 * staff. Presentation only.
 */
public final class MuteView {

    private final Messenger messenger;
    private final Reasons reasons;

    public MuteView(Messenger messenger, Reasons reasons) {
        this.messenger = messenger;
        this.reasons = reasons;
    }

    public void notice(ServerPlayer player, MuteRecord mute, Instant now) {
        Message message = mute.remainingAt(now)
                .map(remaining -> Message.error(ModerationMessages.Mute.NOTICE_TEMPORARY).with("time", Durations.format(remaining)))
                .orElseGet(() -> Message.error(ModerationMessages.Mute.NOTICE_PERMANENT));
        messenger.send(player, message.prefixed().with("reason", reasons.of(mute.reason())));
    }

    /** Tells the staff who can see command feedback. */
    public void muted(CommandSourceStack source, MuteRecord mute, PunishmentTerms terms) {
        Message message = terms.duration()
                .map(duration -> Message.success(ModerationMessages.Mute.MUTED_TEMPORARY).with("time", Durations.format(duration)))
                .orElseGet(() -> Message.success(ModerationMessages.Mute.MUTED_PERMANENT));
        source.sendSuccess(() -> messenger.format(message.prefixed().with("player", mute.name()).with("reason", reasons.of(mute.reason()))), true);
    }

    public void alreadyMuted(CommandSourceStack source) {
        messenger.send(source, Message.error(ModerationMessages.Mute.ALREADY_MUTED).prefixed());
    }

    public void unmuted(CommandSourceStack source, String player) {
        source.sendSuccess(() -> messenger.format(Message.success(ModerationMessages.Mute.UNMUTED).prefixed().with("player", player)), true);
    }

    public void unmutedNotice(ServerPlayer player) {
        messenger.send(player, Message.success(ModerationMessages.Mute.UNMUTED_NOTICE).prefixed());
    }

    public void notMuted(CommandSourceStack source) {
        messenger.send(source, Message.error(ModerationMessages.Mute.NOT_MUTED).prefixed());
    }
}
