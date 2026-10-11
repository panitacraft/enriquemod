package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.ChatPermissions;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.message.ChatView;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BiFunction;

/**
 * What happens when a player writes in a channel: their text is read as far as their permissions allow, and the line
 * goes to everyone who may read the channel. A player who has not entered their PIN yet neither writes nor reads.
 */
public final class ChatService {

    private final ChatTextParser parser;
    private final ChatView view;
    private final HeldPlayers held;
    private final ChannelSelection selection;

    public ChatService(ChatTextParser parser, ChatView view, HeldPlayers held, ChannelSelection selection) {
        this.parser = parser;
        this.view = view;
        this.held = held;
        this.selection = selection;
    }

    /**
     * @param raw what the player typed, exactly as they typed it
     */
    public void speak(ServerPlayer sender, ChannelDefinition channel, String raw) {
        deliver(sender, channel, raw, (text, player) -> view.line(player, channel, text), false);
    }

    /**
     * {@code /me}: what the player does, said in the channel they write in. Only players can.
     *
     * @return how a command reports it: 1 when it was said
     */
    public int emote(CommandSourceStack source, String raw) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            view.playersOnly(source);
            return 0;
        }
        return deliver(sender, selection.current(sender), raw, (text, player) -> view.emoteLine(player, text), true) ? 1 : 0;
    }

    private boolean deliver(ServerPlayer sender, ChannelDefinition channel, String raw,
                            BiFunction<Component, ServerPlayer, Component> lineOf, boolean emote) {
        if (held.isHeld(sender)) {
            return false;
        }
        if (!channel.canUse(sender)) {
            view.channelDenied(sender, channel);
            return false;
        }
        Component text = parser.parse(raw, ChatPermissions.capabilitiesOf(sender));
        if (text.getString().isBlank()) {
            return false;
        }
        MinecraftServer server = sender.level().getServer();
        List<ServerPlayer> readers = server.getPlayerList().getPlayers().stream()
                .filter(player -> channel.canUse(player) && !held.isHeld(player))
                .toList();
        view.deliver(server, readers, lineOf.apply(text, sender));
        ChatEvents.MESSAGE.invoker().onMessage(new ChatEvents.ChatMessage(channel, sender, text.getString(), emote));
        return true;
    }
}
