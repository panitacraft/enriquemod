package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Builds and sends the lines of chat and reports about channels. Presentation only: who gets a line is decided by
 * {@code ChatService}.
 */
public final class ChatView {

    private final Messenger messenger;
    private final NicknameService nicknames;

    public ChatView(Messenger messenger, NicknameService nicknames) {
        this.messenger = messenger;
        this.nicknames = nicknames;
    }

    /** The line a player's text makes in a channel, as the channel's format says. */
    public Component line(ServerPlayer sender, ChannelDefinition channel, Component text) {
        Message line = Message.plain(channel.format())
                .with("sender", senderName(sender))
                .with("message", text)
                .with("channel", channel.id());
        return messenger.format(line, sender);
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

    /** The name a line shows: pointing at it tells who is behind it, clicking it starts a private message. */
    private Component senderName(ServerPlayer sender) {
        String name = sender.getGameProfile().name();
        Component shown = nicknames.nicknameOf(sender.getUUID()).orElseGet(() -> Component.literal(name));
        Component tooltip = messenger.format(Message.plain(ChatMessages.Format.NAME_HOVER)
                .with("nickname", shown)
                .with("name", name), sender);
        return shown.copy().withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowText(tooltip))
                .withClickEvent(new ClickEvent.SuggestCommand("/msg " + name + " ")));
    }
}
