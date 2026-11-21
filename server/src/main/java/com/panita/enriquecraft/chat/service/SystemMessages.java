package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.event.SystemEvent;
import com.panita.enriquecraft.chat.message.SystemMessageView;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Takes over the messages the game broadcasts about players: those that are events are announced to the modules that follow
 * them, and shown in chat the way the administrator wants, or not at all. Players who have not entered their PIN yet read
 * none of them.
 */
public final class SystemMessages {

    private final SystemMessageClassifier classifier;
    private final SystemMessageView view;
    private final ChatConfig config;
    private final HeldPlayers held;
    // The game announces a join before it adds the player to its list, so the one being placed is remembered here.
    private @Nullable ServerPlayer joining;

    public SystemMessages(SystemMessageClassifier classifier, SystemMessageView view, ChatConfig config, HeldPlayers held) {
        this.classifier = classifier;
        this.view = view;
        this.config = config;
        this.held = held;
    }

    /** A player is being placed in the world; the message about it comes before they are in the game's list. */
    public void joinBegins(ServerPlayer player) {
        joining = player;
    }

    public void joinEnds() {
        joining = null;
    }

    /**
     * @return whether the message was one of the events and so must not be broadcast by the game
     */
    public boolean handle(MinecraftServer server, Component message) {
        Optional<SystemEvent> found = classifier.recognize(message)
                .flatMap(recognized -> eventOf(recognized, playerNamed(server, recognized.playerName())));
        if (found.isEmpty()) {
            return false;
        }
        SystemEvent event = found.get();
        ChatEvents.SYSTEM.invoker().onSystemEvent(event);
        boolean announceFirst = event instanceof SystemEvent.Joined joined && joined.first() && config.firstJoinMessage.get();
        if (announceFirst || isShown(event)) {
            List<ServerPlayer> readers = server.getPlayerList().getPlayers().stream().filter(player -> !held.isHeld(player)).toList();
            view.deliver(server, readers, view.line(event, announceFirst));
        }
        return true;
    }

    private @Nullable ServerPlayer playerNamed(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        return online == null && joining != null && joining.getGameProfile().name().equals(name) ? joining : online;
    }

    private static Optional<SystemEvent> eventOf(SystemMessageClassifier.Recognized recognized, @Nullable ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(switch (recognized.kind()) {
            case JOINED -> new SystemEvent.Joined(player, isFirstJoin(player), Optional.empty());
            case JOINED_RENAMED -> new SystemEvent.Joined(player, false, recognized.oldName());
            case LEFT -> new SystemEvent.Left(player);
            case DIED -> new SystemEvent.Died(player, recognized.message());
            case ADVANCED -> new SystemEvent.Advanced(player, recognized.message());
        });
    }

    /** A player who has never left the server has never been here before. */
    private static boolean isFirstJoin(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) == 0;
    }

    private boolean isShown(SystemEvent event) {
        return switch (event) {
            case SystemEvent.Joined joined -> config.joinMessage.get();
            case SystemEvent.Left left -> config.leaveMessage.get();
            case SystemEvent.Died died -> config.deathMessage.get();
            case SystemEvent.Advanced advanced -> config.advancementMessage.get();
        };
    }
}
