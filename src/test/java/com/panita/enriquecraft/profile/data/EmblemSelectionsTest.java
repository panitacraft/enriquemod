package com.panita.enriquecraft.profile.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmblemSelectionsTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    @Test
    void aFileWrittenBeforeTheLevelsBelongedToTheServerIsStillRead() {
        JsonElement legacy = JsonParser.parseString(
                "{\"entries\": [{\"userId\": 7, \"emblems\": [\"a\", \"b\"]}], \"pranks\": [{\"userId\": 7, \"emblem\": \"enrique6_prank_2\"}]}");

        EmblemSelections read = EmblemSelections.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();

        assertEquals(Optional.of(List.of("a", "b")), read.of(7));
        assertEquals(Optional.empty(), read.prankOf(7));
    }

    @Test
    void aChoiceIsReadBackExactly() {
        EmblemSelections selections = EmblemSelections.EMPTY.with(7, List.of("a"))
                .withPrank(7, "enrique6_prank_2", NOW).withPrank(9, "enrique6_prank_1", NOW.plusSeconds(60));

        JsonElement written = EmblemSelections.CODEC.encodeStart(JsonOps.INSTANCE, selections).getOrThrow();

        assertEquals(selections, EmblemSelections.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void thePrankAndTheEmblemsAreChosenApart() {
        EmblemSelections selections = EmblemSelections.EMPTY.withPrank(7, "enrique6_prank_2", NOW).with(7, List.of("a"));

        assertEquals(Optional.of(new EmblemSelections.Prank(7, "enrique6_prank_2", NOW)), selections.prankOf(7));
        assertEquals(Optional.of(new EmblemSelections.Prank(7, "enrique6_prank_2", NOW)), selections.without(7).prankOf(7));
        assertEquals(Optional.empty(), selections.without(7).of(7));
    }

    @Test
    void choosingAgainReplacesThePrankAndItsTime() {
        EmblemSelections selections = EmblemSelections.EMPTY.withPrank(7, "enrique6_prank_1", NOW)
                .withPrank(7, "enrique6_prank_3", NOW.plusSeconds(10));

        assertEquals(Optional.of(new EmblemSelections.Prank(7, "enrique6_prank_3", NOW.plusSeconds(10))), selections.prankOf(7));
        assertEquals(1, selections.pranks().size());
    }
}
