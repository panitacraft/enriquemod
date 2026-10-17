package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The data of the answers the bot asks for. Plain numbers and names in, JSON out, so it needs no running server.
 */
public final class Answers {

    /** A player who is online, with the nickname they have in chat, if any. */
    public record Online(String name, Optional<String> nickname) {
    }

    private Answers() {
    }

    public static JsonObject playerlist(List<Online> online, int max) {
        JsonArray players = new JsonArray();
        for (Online player : online) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", player.name());
            entry.addProperty("nickname", player.nickname().orElse(null));
            players.add(entry);
        }
        JsonObject data = new JsonObject();
        data.add("online", players);
        data.addProperty("max", max);
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
