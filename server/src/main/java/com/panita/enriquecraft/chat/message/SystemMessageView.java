package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.event.SystemEvent;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Builds and sends the lines that replace the game's messages about players. Presentation only: what is shown is decided by
 * {@code SystemMessages}.
 */
public final class SystemMessageView {

    private final Messenger messenger;

    public SystemMessageView(Messenger messenger) {
        this.messenger = messenger;
    }

    /**
     * @param announceFirst whether a join is the first one of the player and has the message for that
     */
    public Component line(SystemEvent event, boolean announceFirst) {
        Component player = event.player().getDisplayName();
        Message message = switch (event) {
            case SystemEvent.Joined joined when announceFirst -> Message.plain(ChatMessages.Events.JOIN_FIRST).with("player", player);
            case SystemEvent.Joined joined when joined.oldName().isPresent() -> Message.plain(ChatMessages.Events.JOIN_RENAMED)
                    .with("player", player).with("oldName", joined.oldName().get());
            case SystemEvent.Joined joined -> Message.plain(ChatMessages.Events.JOIN).with("player", player);
            case SystemEvent.Left left -> Message.plain(ChatMessages.Events.LEAVE).with("player", player);
            case SystemEvent.Died died -> Message.plain(ChatMessages.Events.DEATH).with("player", player).with("message", died.message());
            case SystemEvent.Advanced advanced -> Message.plain(ChatMessages.Events.ADVANCEMENT)
                    .with("player", player).with("message", advanced.message());
        };
        return messenger.format(message, event.player());
    }

    public void deliver(MinecraftServer server, Collection<ServerPlayer> readers, Component line) {
        messenger.deliver(server, readers, line);
    }
}
