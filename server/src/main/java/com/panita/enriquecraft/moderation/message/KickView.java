package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The disconnect screen of a kicked player. Presentation only.
 */
public final class KickView {

    private final Messenger messenger;
    private final Reasons reasons;

    public KickView(Messenger messenger, Reasons reasons) {
        this.messenger = messenger;
        this.reasons = reasons;
    }

    /** The prefix, and below it the reason as staff wrote it; nothing else. */
    public Component screen(Optional<Component> reason) {
        return messenger.format(Message.plain(ModerationMessages.Kick.SCREEN).with("reason", reason.orElseGet(reasons::none)));
    }
}
