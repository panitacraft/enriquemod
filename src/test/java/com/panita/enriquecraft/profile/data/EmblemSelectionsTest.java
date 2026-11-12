package com.panita.enriquecraft.profile.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmblemSelectionsTest {

    @Test
    void aFileWrittenBeforeThePrankCouldBeChosenIsStillRead() {
        JsonElement legacy = JsonParser.parseString("{\"entries\": [{\"userId\": 7, \"emblems\": [\"a\", \"b\"]}]}");

        EmblemSelections read = EmblemSelections.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();

        assertEquals(Optional.of(List.of("a", "b")), read.of(7));
        assertEquals(Optional.empty(), read.prankOf(7));
    }

    @Test
    void aChoiceIsReadBackExactly() {
        EmblemSelections selections = EmblemSelections.EMPTY.with(7, List.of("a")).withPrank(7, "enrique6_prank_2").withPrank(9, "enrique6_prank_1");

        JsonElement written = EmblemSelections.CODEC.encodeStart(JsonOps.INSTANCE, selections).getOrThrow();

        assertEquals(selections, EmblemSelections.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void thePrankAndTheEmblemsAreChosenApart() {
        EmblemSelections selections = EmblemSelections.EMPTY.withPrank(7, "enrique6_prank_2").with(7, List.of("a"));

        assertEquals(Optional.of("enrique6_prank_2"), selections.prankOf(7));
        assertEquals(Optional.of("enrique6_prank_2"), selections.without(7).prankOf(7));
        assertEquals(Optional.empty(), selections.without(7).of(7));
        assertEquals(Optional.of(List.of("a")), selections.withoutPrank(7).of(7));
        assertEquals(Optional.empty(), selections.withoutPrank(7).prankOf(7));
    }

    @Test
    void choosingAgainReplacesThePrank() {
        EmblemSelections selections = EmblemSelections.EMPTY.withPrank(7, "enrique6_prank_1").withPrank(7, "enrique6_prank_3");

        assertEquals(Optional.of("enrique6_prank_3"), selections.prankOf(7));
        assertEquals(1, selections.pranks().size());
    }
}
