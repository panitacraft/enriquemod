package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.ui.ChatPrompts;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Gives a pending chat prompt the next message of its player, and forgets it when they leave.
 */
public final class ChatPromptListener implements ModListener {

    private final ChatPrompts prompts;

    public ChatPromptListener(ChatPrompts prompts) {
        this.prompts = prompts;
    }

    @Override
    public void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, params) -> !prompts.answer(sender.getUUID(), message.signedContent()));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> prompts.forget(handler.getPlayer().getUUID()));
    }
}
