package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

/**
 * The data of the answers the bot asks for. Plain numbers and names in, JSON out, so it needs no running server.
 */
public final class Answers {

    /**
     * A player who is online: who they are, their latency in milliseconds and the line the server shows for them, as
     * {@link DisplayRuns}.
     */
    public record Online(PlayerInfo player, int ping, JsonArray display) {
    }

    /**
     * What the server knows about one player, for {@code playerinfo}. A player who is not online has no ping, and their line is
     * only their name, since their rank is only known to the server while they are connected.
     */
    public record Info(PlayerInfo player, boolean online, OptionalInt ping, JsonArray display, OptionalLong playtimeSeconds) {
    }

    private Answers() {
    }

    private static JsonObject describe(PlayerInfo player, JsonArray display) {
        JsonObject entry = new JsonObject();
        entry.addProperty("name", player.name());
        entry.addProperty("nickname", player.nickname().orElse(null));
        entry.addProperty("uuid", player.uuid().toString());
        entry.addProperty("premium", player.premium());
        player.skin().ifPresent(skin -> entry.addProperty("skin", skin));
        entry.add("display", display);
        return entry;
    }

    public static JsonObject playerlist(List<Online> online, int max) {
        JsonArray players = new JsonArray();
        for (Online player : online) {
            JsonObject entry = describe(player.player(), player.display());
            entry.addProperty("ping", player.ping());
            players.add(entry);
        }
        JsonObject data = new JsonObject();
        data.add("online", players);
        data.addProperty("max", max);
        return data;
    }

    /** The answer to {@code playerinfo}: {@code {found: false}} for a name the server does not know. */
    public static JsonObject playerinfo(Optional<Info> info) {
        if (info.isEmpty()) {
            JsonObject none = new JsonObject();
            none.addProperty("found", false);
            return none;
        }
        JsonObject data = describe(info.get().player(), info.get().display());
        data.addProperty("found", true);
        data.addProperty("online", info.get().online());
        info.get().ping().ifPresent(ping -> data.addProperty("ping", ping));
        info.get().playtimeSeconds().ifPresent(seconds -> data.addProperty("playtimeSeconds", seconds));
        return data;
    }

    /**
     * The ticks per second the server really achieves: its own rate while it keeps up, less when a tick takes longer than the
     * time it has.
     *
     * @param mspt     the average duration of a tick, in milliseconds
     * @param tickRate the ticks per second the server aims at
     */
    public static double tps(double mspt, double tickRate) {
        return mspt <= 0 ? tickRate : Math.min(tickRate, 1000.0 / mspt);
    }

    public static JsonObject performance(double mspt, double tickRate) {
        JsonObject data = new JsonObject();
        data.addProperty("tps", round(tps(mspt, tickRate)));
        data.addProperty("mspt", round(mspt));
        return data;
    }

    /** How many different players these names stand for. Names are compared without regard to capitalization. */
    public static int distinctPlayers(Collection<String> names) {
        Set<String> distinct = new HashSet<>();
        for (String name : names) {
            distinct.add(name.toLowerCase(Locale.ROOT));
        }
        return distinct.size();
    }

    /** @param totalPlayers every player the server knows, online or not; not the online count */
    public static JsonObject status(int online, int max, int totalPlayers, double mspt, double tickRate, long uptimeSeconds,
                                    String version) {
        JsonObject data = performance(mspt, tickRate);
        data.addProperty("online", online);
        data.addProperty("max", max);
        data.addProperty("totalPlayers", totalPlayers);
        data.addProperty("uptimeSeconds", uptimeSeconds);
        data.addProperty("version", version);
        return data;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
