package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.moderation.service.PunishmentTerms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * What a ban shows: the screen of the banned player, who sees it when they are kicked out and each time they try to come back,
 * and the report to staff. Presentation only.
 */
public final class BanView {

    private final Messenger messenger;
    private final Reasons reasons;

    public BanView(Messenger messenger, Reasons reasons) {
        this.messenger = messenger;
        this.reasons = reasons;
    }

    /**
     * @param expires when the ban ends; empty for a permanent one
     */
    public Component screen(Optional<Instant> expires, Optional<String> reason, Instant now) {
        Component why = reasons.of(reason);
        return messenger.format(expires
                .map(end -> Message.plain(ModerationMessages.Ban.SCREEN_TEMPORARY).with("time", Durations.format(Duration.between(now, end))))
                .orElseGet(() -> Message.plain(ModerationMessages.Ban.SCREEN_PERMANENT))
                .with("reason", why));
    }

    /** Tells the staff who can see command feedback, like the game's own ban does. */
    public void banned(CommandSourceStack source, String player, PunishmentTerms terms) {
        Message message = terms.duration()
                .map(duration -> Message.success(ModerationMessages.Ban.BANNED_TEMPORARY).with("time", Durations.format(duration)))
                .orElseGet(() -> Message.success(ModerationMessages.Ban.BANNED_PERMANENT));
        source.sendSuccess(() -> messenger.format(message.prefixed().with("player", player).with("reason", reasons.of(terms.reason()))), true);
    }

    public void alreadyBanned(CommandSourceStack source) {
        messenger.send(source, Message.error(ModerationMessages.Ban.ALREADY_BANNED).prefixed());
    }
}
