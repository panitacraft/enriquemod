package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonObject;
import com.panita.enriquecraft.chat.service.KnownPlayers;
import com.panita.enriquecraft.discord.protocol.InFrame;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Answers what the bot asks about the server: who is online, how fast it runs, and its status. Runs on the server thread. A
 * player who has not entered their PIN yet is not counted, since they are not really in.
 */
public final class QueryAnswers {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryAnswers.class);
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    private final FrameSink sink;
    private final ServerInfo info;
    private final PlayerRoster roster;
    private final KnownPlayers known;
    private final Supplier<Instant> clock;

    public QueryAnswers(FrameSink sink, ServerInfo info, PlayerRoster roster, KnownPlayers known, Supplier<Instant> clock) {
        this.sink = sink;
        this.info = info;
        this.roster = roster;
        this.known = known;
        this.clock = clock;
    }

    public void answer(MinecraftServer server, InFrame.Query query) {
        try {
            sink.send(OutFrame.Reply.ok(query.id(), data(server, query)));
        } catch (RuntimeException e) {
            LOGGER.error("Could not answer a question of the bot", e);
            sink.send(OutFrame.Reply.failed(query.id(), "error"));
        }
    }

    private JsonObject data(MinecraftServer server, InFrame.Query query) {
        return switch (query.what()) {
            case PLAYERLIST -> Answers.playerlist(roster.online(server), server.getMaxPlayers());
            case PLAYERINFO -> Answers.playerinfo(query.player().flatMap(name -> roster.info(server, name)));
            case TPS -> Answers.performance(mspt(server), tickRate(server));
            case STATUS -> Answers.status(roster.online(server).size(), server.getMaxPlayers(),
                    Answers.distinctPlayers(known.namesOn(server)), mspt(server), tickRate(server), uptimeSeconds(),
                    server.getServerVersion());
        };
    }

    private static double mspt(MinecraftServer server) {
        return server.getAverageTickTimeNanos() / NANOS_PER_MILLISECOND;
    }

    private static double tickRate(MinecraftServer server) {
        return server.tickRateManager().tickrate();
    }

    private long uptimeSeconds() {
        return info.startedAt().map(started -> Duration.between(started, clock.get()).toSeconds()).orElse(0L);
    }
}
