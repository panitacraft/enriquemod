package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.service.ChatHeads;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Builds and sends the lines of chat and reports about channels. Presentation only: who gets a line is decided by
 * {@code ChatService}.
 */
public final class ChatView {

    private final Messenger messenger;
    private final SenderNameView names;
    private final ChatHeads heads;

    public ChatView(Messenger messenger, SenderNameView names, ChatHeads heads) {
        this.messenger = messenger;
        this.names = names;
        this.heads = heads;
    }

    /** The line a player's text makes in a channel, as the channel's format says, after the head of the player. */
    public Component line(ServerPlayer sender, ChannelDefinition channel, Component text) {
        Message line = Message.plain(channel.format())
                .with("sender", names.of(sender))
                .with("message", text)
                .with("channel", channel.id());
        return withHead(sender, messenger.format(line, sender));
    }

    /**
     * The line a message from outside the game makes in a channel. The sender is already a finished component and no player
     * stands behind the line, so no placeholder of a player is resolved.
     */
    public Component externalLine(ChannelDefinition channel, Component sender, Component text) {
        String template = channel.id().equals(ChannelDefinition.STAFF) ? ChatMessages.Format.EXTERNAL_STAFF : ChatMessages.Format.EXTERNAL;
        return messenger.format(Message.plain(template).with("sender", sender).with("message", text));
    }

    /** The line of a player's {@code /me}. */
    public Component emoteLine(ServerPlayer sender, Component text) {
        return withHead(sender, messenger.format(Message.plain(ChatMessages.Format.EMOTE).with("sender", names.of(sender)).with("message", text), sender));
    }

    private Component withHead(ServerPlayer sender, Component line) {
        return heads.of(sender).<Component>map(head -> Component.empty().append(head).append(line)).orElse(line);
    }

    /** Delivers a line to the readers and to the console. */
    public void deliver(MinecraftServer server, Collection<ServerPlayer> readers, Component line) {
        messenger.deliver(server, readers, line);
    }

    public void switched(ServerPlayer player, ChannelDefinition channel) {
        messenger.send(player, Message.success(ChatMessages.Channel.SWITCHED).prefixed().with("channel", channel.id()));
    }

    public void current(ServerPlayer player, ChannelDefinition channel) {
        messenger.send(player, Message.info(ChatMessages.Channel.CURRENT).prefixed().with("channel", channel.id()));
    }

    public void unknownChannel(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(ChatMessages.Channel.UNKNOWN).prefixed().with("channel", name));
    }

    public void channelDenied(ServerPlayer player, ChannelDefinition channel) {
        messenger.send(player, Message.error(ChatMessages.Channel.DENIED).prefixed().with("channel", channel.id()));
    }

    public void playersOnly(CommandSourceStack source) {
        messenger.send(source, Message.error(Messages.Command.PLAYERS_ONLY).prefixed());
    }
}
