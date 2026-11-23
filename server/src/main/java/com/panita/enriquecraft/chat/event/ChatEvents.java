package com.panita.enriquecraft.chat.event;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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

    /** A player started or stopped being AFK. Never called for a player on the PIN screen or one who never shows as AFK. */
    public static final Event<AfkChanged> AFK = EventFactory.createArrayBacked(AfkChanged.class,
            listeners -> (player, afk) -> {
                for (AfkChanged listener : listeners) {
                    listener.onAfkChanged(player, afk);
                }
            });

    /** A player told everyone where they are; called after the line was delivered. Not called when the position went to one player. */
    public static final Event<CoordinatesShared> COORDINATES = EventFactory.createArrayBacked(CoordinatesShared.class,
            listeners -> (player, position, dimension) -> {
                for (CoordinatesShared listener : listeners) {
                    listener.onCoordinatesShared(player, position, dimension);
                }
            });

    @FunctionalInterface
    public interface CoordinatesShared {

        void onCoordinatesShared(ServerPlayer player, BlockPos position, ResourceKey<Level> dimension);
    }

    @FunctionalInterface
    public interface AfkChanged {

        void onAfkChanged(ServerPlayer player, boolean afk);
    }

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
     * @param emote   whether it is what they do, written with {@code /me}, rather than what they say
     */
    public record ChatMessage(ChannelDefinition channel, ServerPlayer sender, String text, boolean emote) {
    }
}
