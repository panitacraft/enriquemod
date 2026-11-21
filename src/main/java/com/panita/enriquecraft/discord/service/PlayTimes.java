package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * How long a player has played and how often they died, from the statistics file the game keeps for each player in the world. The game only writes it
 * now and then, so for a player who is online the live statistic is used instead; this is for the ones who are not.
 */
public final class PlayTimes {

    private static final long TICKS_PER_SECOND = 20;

    private PlayTimes() {
    }

    /** The seconds in the statistics file of the player, or empty when there is none or it holds no play time. */
    public static OptionalLong secondsOf(Path statsFolder, UUID player) {
        Path file = statsFolder.resolve(player + ".json");
        try {
            return secondsIn(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return OptionalLong.empty();
        }
    }

    /** The number of deaths in the statistics file of the player, or empty when there is none or it holds no deaths. */
    public static OptionalLong deathsOf(Path statsFolder, UUID player) {
        try {
            return customStat(Files.readString(statsFolder.resolve(player + ".json"), StandardCharsets.UTF_8), "minecraft:deaths");
        } catch (IOException e) {
            return OptionalLong.empty();
        }
    }

    /** The play time of a statistics file's text, in seconds; the file holds ticks. Anything that is not the usual shape is empty. */
    static OptionalLong secondsIn(String json) {
        OptionalLong ticks = customStat(json, "minecraft:play_time");
        return ticks.isPresent() ? OptionalLong.of(ticks.getAsLong() / TICKS_PER_SECOND) : OptionalLong.empty();
    }

    /** One counter of the file's custom statistics; empty when it is missing, negative or the file is not the usual shape. */
    private static OptionalLong customStat(String json, String key) {
        try {
            JsonElement value = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("stats")
                    .getAsJsonObject("minecraft:custom").get(key);
            return value != null && value.isJsonPrimitive() && value.getAsLong() >= 0 ? OptionalLong.of(value.getAsLong()) : OptionalLong.empty();
        } catch (RuntimeException e) {
            return OptionalLong.empty();
        }
    }

    public static long secondsOfTicks(int ticks) {
        return Math.max(0, ticks) / TICKS_PER_SECOND;
    }
}
