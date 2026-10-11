package com.panita.enriquecraft.chat.event;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * What happens in chat, for the modules that follow it, such as the one that mirrors chat to Discord. Listeners are
 * called on the server thread and must not hold on to the players.
 */
public final class ChatEvents {

    private ChatEvents() {
    }

    /** A player wrote in a channel; called after the line was delivered. */
    public static final Event<MessageSent> MESSAGE = EventFactory.createArrayBacked(MessageSent.class,
            listeners -> message -> {
                for (MessageSent listener : listeners) {
                    listener.onMessage(message);
                }
            });

    /**
     * The game told everyone about a player. Called whether or not the administrator shows that kind of message in chat.
     */
    public static final Event<SystemEventHappened> SYSTEM = EventFactory.createArrayBacked(SystemEventHappened.class,
            listeners -> event -> {
                for (SystemEventHappened listener : listeners) {
                    listener.onSystemEvent(event);
                }
            });

    @FunctionalInterface
    public interface MessageSent {

        void onMessage(ChatMessage message);
    }

    @FunctionalInterface
    public interface SystemEventHappened {

        void onSystemEvent(SystemEvent event);
    }

    /**
     * @param channel the channel the line was written in
     * @param sender  who wrote it
     * @param text    what they wrote, as plain text
     */
    public record ChatMessage(ChannelDefinition channel, ServerPlayer sender, String text) {
    }
}
