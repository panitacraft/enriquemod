package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The reason of a punishment as text: what staff wrote, shown as typed and never read as formatting, or the words that
 * stand for no reason.
 */
public final class Reasons {

    private final Messenger messenger;

    public Reasons(Messenger messenger) {
        this.messenger = messenger;
    }

    public Component of(Optional<String> reason) {
        return reason.<Component>map(Component::literal).orElseGet(this::none);
    }

    public Component none() {
        return messenger.format(Message.plain(ModerationMessages.Common.NO_REASON));
    }
}
