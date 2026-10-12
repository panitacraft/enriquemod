package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.EventKind;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells the bot what happens in the game. A join is reported when the player is ready, which for a player who has to enter
 * a PIN is later than when they connected, and a leave only for players who were ready, so nobody is announced who was never
 * really in. Text comes in as it is and leaves as plain text of a size the protocol allows.
 */
public final class GameReporter {

    private final FrameSink sink;
    private final Set<UUID> ready = ConcurrentHashMap.newKeySet();
    private final Set<UUID> arrivedForTheFirstTime = ConcurrentHashMap.newKeySet();

    public GameReporter(FrameSink sink) {
        this.sink = sink;
    }

    /** A player wrote in a chat channel, or did something with {@code /me}. */
    public void chat(String channel, PlayerInfo player, String text, boolean emote) {
        String id = channel.toLowerCase(Locale.ROOT);
        String plain = PlainText.chat(text);
        if (plain.isEmpty() || !sink.bridges(id)) {
            return;
        }
        sink.send(new OutFrame.Chat(id, player, plain, emote));
    }

    /** A player connected. It is remembered if it is their first time, to say so when they are ready. */
    public void arrived(UUID player, boolean firstTime) {
        if (firstTime) {
            arrivedForTheFirstTime.add(player);
        }
    }

    /** A player is in and may play. */
    public void ready(PlayerInfo player) {
        boolean first = arrivedForTheFirstTime.remove(player.uuid());
        ready.add(player.uuid());
        event(first ? EventKind.FIRST_JOIN : EventKind.JOIN, Optional.of(player), Optional.empty(), Optional.empty());
    }

    public void left(PlayerInfo player) {
        arrivedForTheFirstTime.remove(player.uuid());
        if (ready.remove(player.uuid())) {
            event(EventKind.LEAVE, Optional.of(player), Optional.empty(), Optional.empty());
        }
    }

    /**
     * @param text the game's own text about the death
     * @param key  its translation key
     */
    public void died(PlayerInfo player, String text, Optional<String> key) {
        event(EventKind.DEATH, Optional.of(player), Optional.of(PlainText.event(text)), key);
    }

    public void advanced(PlayerInfo player, String text, Optional<String> key) {
        event(EventKind.ADVANCEMENT, Optional.of(player), Optional.of(PlainText.event(text)), key);
    }

    /** @param raw what staff wrote, which may carry text tags and color codes */
    public void broadcast(String raw) {
        String plain = PlainText.event(PlainText.withoutMarkup(raw));
        if (!plain.isEmpty()) {
            event(EventKind.BROADCAST, Optional.empty(), Optional.of(plain), Optional.empty());
        }
    }

    public void started() {
        event(EventKind.STARTED, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void stopping() {
        event(EventKind.STOPPING, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private void event(EventKind kind, Optional<PlayerInfo> player, Optional<String> text, Optional<String> key) {
        sink.send(new OutFrame.Event(kind, player, text, key));
    }
}
