package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.ChatPermissions;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.message.ChatView;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * What happens when a player writes in a channel: their text is read as far as their permissions allow, and the line
 * goes to everyone who may read the channel. A player who has not entered their PIN yet neither writes nor reads.
 */
public final class ChatService {

    private final ChatTextParser parser;
    private final ChatView view;
    private final HeldPlayers held;

    public ChatService(ChatTextParser parser, ChatView view, HeldPlayers held) {
        this.parser = parser;
        this.view = view;
        this.held = held;
    }

    /**
     * @param raw what the player typed, exactly as they typed it
     */
    public void speak(ServerPlayer sender, ChannelDefinition channel, String raw) {
        if (held.isHeld(sender)) {
            return;
        }
        if (!channel.canUse(sender)) {
            view.channelDenied(sender, channel);
            return;
        }
        Component text = parser.parse(raw, ChatPermissions.capabilitiesOf(sender));
        if (text.getString().isBlank()) {
            return;
        }
        MinecraftServer server = sender.level().getServer();
        List<ServerPlayer> readers = server.getPlayerList().getPlayers().stream()
                .filter(player -> channel.canUse(player) && !held.isHeld(player))
                .toList();
        view.deliver(server, readers, view.line(sender, channel, text));
        ChatEvents.MESSAGE.invoker().onMessage(new ChatEvents.ChatMessage(channel, sender, text.getString()));
    }
}
