package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.EventKind;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import com.panita.enriquecraft.discord.protocol.Protocol;

import java.util.List;
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
    private final Mentions mentions;

    public GameReporter(FrameSink sink, Mentions mentions) {
        this.sink = sink;
        this.mentions = mentions;
    }

    /**
     * A player wrote in a chat channel, or did something with {@code /me}.
     *
     * @param mayMention whether the player may ping linked Discord members with {@code @name}; it only has an effect on a bot that
     *                   offers the feature, since one that does not would show the text as it is
     */
    public void chat(String channel, PlayerInfo player, String text, boolean emote, boolean mayMention) {
        String id = channel.toLowerCase(Locale.ROOT);
        String plain = PlainText.chat(text);
        if (plain.isEmpty() || !sink.bridges(id)) {
            return;
        }
        // Looked for in the text that travels, so a mention cut off by the size limit is not sent
        List<OutFrame.Mention> mentioned = mayMention && sink.offers(Protocol.FEATURE_MENTIONS) ? mentions.in(plain) : List.of();
        sink.send(new OutFrame.Chat(id, player, plain, emote, mentioned));
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

    /**
     * A player became AFK or came back. Only told to a bot that offers the feature, since one that does not know the kinds could
     * refuse the frame, and only for players who were reported as in.
     */
    public void afk(PlayerInfo player, boolean afk) {
        if (ready.contains(player.uuid()) && sink.offers(Protocol.FEATURE_AFK)) {
            event(afk ? EventKind.AFK : EventKind.BACK, Optional.of(player), Optional.empty(), Optional.empty());
        }
    }

    /**
     * A player told everyone where they are. Only told to a bot that offers the feature, and only for players who were reported as in.
     *
     * @param position the position as the game's players read it, such as {@code X: 10 Y: 64 Z: -20 (Overworld)}
     */
    public void coordinates(PlayerInfo player, String position) {
        if (ready.contains(player.uuid()) && sink.offers(Protocol.FEATURE_COORDINATES)) {
            event(EventKind.COORDINATES, Optional.of(player), Optional.of(PlainText.event(position)), Optional.empty());
        }
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
