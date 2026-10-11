package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * The welcome message a player gets once they are in.
 */
public final class WelcomeView {

    private final Messenger messenger;

    public WelcomeView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void send(ServerPlayer player) {
        messenger.deliver(List.of(player), messenger.format(
                Message.plain(ChatMessages.Welcome.TEXT).with("player", player.getDisplayName()), player));
    }
}
