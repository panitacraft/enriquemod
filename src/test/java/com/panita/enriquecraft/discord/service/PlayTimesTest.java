package com.panita.enriquecraft.discord.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayTimesTest {

    private static final String STATS = "{\"stats\":{\"minecraft:custom\":{\"minecraft:play_time\":72000,\"minecraft:jump\":5}},\"DataVersion\":1}";

    @Test
    void theFileHoldsTicksAndTheAnswerIsSeconds() {
        assertEquals(OptionalLong.of(3600), PlayTimes.secondsIn(STATS));
        assertEquals(OptionalLong.of(0), PlayTimes.secondsIn(STATS.replace("72000", "19")));
    }

    @Test
    void anythingButTheUsualShapeIsEmpty() {
        assertEquals(OptionalLong.empty(), PlayTimes.secondsIn("{}"));
        assertEquals(OptionalLong.empty(), PlayTimes.secondsIn("not json"));
        assertEquals(OptionalLong.empty(), PlayTimes.secondsIn("{\"stats\":{\"minecraft:custom\":{\"minecraft:jump\":5}}}"));
        assertEquals(OptionalLong.empty(), PlayTimes.secondsIn(STATS.replace("72000", "-5")));
        assertEquals(OptionalLong.empty(), PlayTimes.secondsIn(STATS.replace("72000", "\"many\"")));
    }

    @Test
    void readsThePlayersFileAndIsEmptyWithoutOne(@TempDir Path folder) throws IOException {
        UUID player = UUID.randomUUID();
        assertEquals(OptionalLong.empty(), PlayTimes.secondsOf(folder, player));
        Files.writeString(folder.resolve(player + ".json"), STATS);
        assertEquals(OptionalLong.of(3600), PlayTimes.secondsOf(folder, player));
    }

    @Test
    void aLiveStatisticInTicksIsSeconds() {
        assertEquals(60, PlayTimes.secondsOfTicks(1200));
        assertEquals(0, PlayTimes.secondsOfTicks(-3));
    }
}
