package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswersTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String SKIN = "a".repeat(64);

    private static PlayerInfo player(String name, Optional<String> nickname, Optional<String> skin) {
        return new PlayerInfo(ID, name, nickname, true, skin);
    }

    @Test
    void theTpsIsTheRateWhileTheServerKeepsUp() {
        assertEquals(20.0, Answers.tps(12.0, 20.0));
        assertEquals(20.0, Answers.tps(50.0, 20.0));
        assertEquals(20.0, Answers.tps(0.0, 20.0));
    }

    @Test
    void theTpsFallsWhenATickTakesLongerThanTheTimeItHas() {
        assertEquals(10.0, Answers.tps(100.0, 20.0));
        assertEquals(40.0, Answers.tps(25.0, 100.0));
    }

    @Test
    void theNumbersAreRoundedToTwoDecimals() {
        JsonObject data = Answers.performance(53.3333, 20.0);

        assertEquals(18.75, data.get("tps").getAsDouble());
        assertEquals(53.33, data.get("mspt").getAsDouble());
    }

    @Test
    void thePlayerListHasANameAndANicknameOrNull() {
        JsonObject data = Answers.playerlist(List.of(new Answers.Online(player("Javithor360", Optional.of("Javi"), Optional.of(SKIN)), 42, new JsonArray()),
                new Answers.Online(player("Tezzlar", Optional.empty(), Optional.empty()), 7, new JsonArray())), 20);

        assertEquals(20, data.get("max").getAsInt());
        assertEquals(2, data.getAsJsonArray("online").size());
        JsonObject first = data.getAsJsonArray("online").get(0).getAsJsonObject();
        assertEquals("Javi", first.get("nickname").getAsString());
        assertEquals(42, first.get("ping").getAsInt());
        assertEquals(SKIN, first.get("skin").getAsString());
        assertEquals(ID.toString(), first.get("uuid").getAsString());
        assertTrue(data.getAsJsonArray("online").get(1).getAsJsonObject().get("nickname").isJsonNull());
        assertFalse(data.getAsJsonArray("online").get(1).getAsJsonObject().has("skin"));
    }

    @Test
    void theInfoOfAnOnlinePlayerHasTheirPingAndPlayTime() {
        JsonObject data = Answers.playerinfo(Optional.of(new Answers.Info(player("Javithor360", Optional.empty(), Optional.empty()), true,
                OptionalInt.of(30), new JsonArray(), OptionalLong.of(3600))));

        assertTrue(data.get("found").getAsBoolean());
        assertTrue(data.get("online").getAsBoolean());
        assertEquals(30, data.get("ping").getAsInt());
        assertEquals(3600, data.get("playtimeSeconds").getAsLong());
    }

    @Test
    void theInfoOfAPlayerWhoIsAwayHasNeitherPingNorPlayTimeWhenUnknown() {
        JsonObject data = Answers.playerinfo(Optional.of(new Answers.Info(player("Tezzlar", Optional.empty(), Optional.empty()), false,
                OptionalInt.empty(), new JsonArray(), OptionalLong.empty())));

        assertFalse(data.get("online").getAsBoolean());
        assertFalse(data.has("ping"));
        assertFalse(data.has("playtimeSeconds"));
    }

    @Test
    void aNameTheServerDoesNotKnowIsNotFound() {
        JsonObject data = Answers.playerinfo(Optional.empty());

        assertFalse(data.get("found").getAsBoolean());
        assertEquals(1, data.size());
    }

    @Test
    void anEmptyServerHasAnEmptyPlayerList() {
        JsonObject data = Answers.playerlist(List.of(), 20);

        assertEquals(0, data.getAsJsonArray("online").size());
    }

    @Test
    void theStatusCarriesEverythingTheBotShows() {
        JsonObject data = Answers.status(3, 20, 57, 12.5, 20.0, 3600, "26.3");

        assertEquals(3, data.get("online").getAsInt());
        assertEquals(20, data.get("max").getAsInt());
        assertEquals(57, data.get("totalPlayers").getAsInt());
        assertEquals(20.0, data.get("tps").getAsDouble());
        assertEquals(12.5, data.get("mspt").getAsDouble());
        assertEquals(3600, data.get("uptimeSeconds").getAsLong());
        assertEquals("26.3", data.get("version").getAsString());
    }

    @Test
    void aPlayerKnownTwiceIsCountedOnceWhateverTheCapitalization() {
        // Online and on the whitelist at once, as a PIN player who is connected is
        assertEquals(3, Answers.distinctPlayers(List.of("Javithor360", "Tezzlar", "javithor360", "Panita", "TEZZLAR")));
    }

    @Test
    void noKnownPlayersCountsZero() {
        assertEquals(0, Answers.distinctPlayers(List.of()));
    }
}
