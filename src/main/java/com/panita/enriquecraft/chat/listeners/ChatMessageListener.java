package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.chat.service.ReplyTracker;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Takes over the chat of players: the game's own broadcast is stopped and the module sends the line itself, so it looks
 * as the channel says. Core registers its own handler first, so a message that answers a prompt never gets here.
 */
public final class ChatMessageListener implements ModListener {

    private final ChatService chat;
    private final ChannelSelection selection;
    private final ReplyTracker replies;

    public ChatMessageListener(ChatService chat, ChannelSelection selection, ReplyTracker replies) {
        this.chat = chat;
        this.selection = selection;
        this.replies = replies;
    }

    @Override
    public void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            chat.speak(sender, selection.current(sender), message.signedContent());
            return false;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            selection.forget(handler.getPlayer().getUUID());
            replies.forget(handler.getPlayer().getUUID());
        });
    }
}
