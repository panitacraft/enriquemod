package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Builds and sends private messages and reports about them. Presentation only.
 */
public final class PrivateMessageView {

    private final Messenger messenger;
    private final SenderNameView names;

    public PrivateMessageView(Messenger messenger, SenderNameView names) {
        this.messenger = messenger;
        this.names = names;
    }

    /** What the one who receives the message sees; the sender can be the console. */
    public void incoming(CommandSourceStack source, ServerPlayer target, Component text) {
        Component sender = source.getPlayer() == null ? Component.literal(source.getTextName()) : names.of(source.getPlayer());
        messenger.deliver(List.of(target), messenger.format(
                Message.plain(ChatMessages.Format.PRIVATE_INCOMING).with("sender", sender).with("message", text), target));
    }

    /** What the one who writes the message sees. */
    public void outgoing(CommandSourceStack source, ServerPlayer target, Component text) {
        messenger.send(source, Message.plain(ChatMessages.Format.PRIVATE_OUTGOING).with("target", names.of(target)).with("message", text));
    }

    public void notAvailable(CommandSourceStack source) {
        messenger.send(source, Message.error(ChatMessages.Private.NOT_AVAILABLE).prefixed());
    }

    public void nobodyToReplyTo(CommandSourceStack source) {
        messenger.send(source, Message.warning(ChatMessages.Private.NOBODY_TO_REPLY_TO).prefixed());
    }
}
