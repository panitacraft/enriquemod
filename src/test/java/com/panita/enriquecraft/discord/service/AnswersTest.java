package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswersTest {

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
        JsonObject data = Answers.playerlist(List.of(new Answers.Online("Javithor360", Optional.of("Javi")),
                new Answers.Online("Tezzlar", Optional.empty())), 20);

        assertEquals(20, data.get("max").getAsInt());
        assertEquals(2, data.getAsJsonArray("online").size());
        assertEquals("Javi", data.getAsJsonArray("online").get(0).getAsJsonObject().get("nickname").getAsString());
        assertTrue(data.getAsJsonArray("online").get(1).getAsJsonObject().get("nickname").isJsonNull());
    }

    @Test
    void anEmptyServerHasAnEmptyPlayerList() {
        JsonObject data = Answers.playerlist(List.of(), 20);

        assertEquals(0, data.getAsJsonArray("online").size());
    }

    @Test
    void theStatusCarriesEverythingTheBotShows() {
        JsonObject data = Answers.status(3, 20, 12.5, 20.0, 3600, "26.3");

        assertEquals(3, data.get("online").getAsInt());
        assertEquals(20, data.get("max").getAsInt());
        assertEquals(20.0, data.get("tps").getAsDouble());
        assertEquals(12.5, data.get("mspt").getAsDouble());
        assertEquals(3600, data.get("uptimeSeconds").getAsLong());
        assertEquals("26.3", data.get("version").getAsString());
    }
}
