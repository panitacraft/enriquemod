package com.panita.enriquecraft.profile.service;

import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmblemSelectionTest {

    @Test
    void anEmblemIsAddedAtTheEndWhenThereIsRoom() {
        EmblemSelection.Toggled toggled = EmblemSelection.toggle(List.of("a"), "b", 3);

        assertEquals(List.of("a", "b"), toggled.emblems());
        assertEquals(EmblemSelection.Change.ADDED, toggled.change());
    }

    @Test
    void anEmblemThatIsShownIsPutAway() {
        EmblemSelection.Toggled toggled = EmblemSelection.toggle(List.of("a", "b", "c"), "b", 3);

        assertEquals(List.of("a", "c"), toggled.emblems());
        assertEquals(EmblemSelection.Change.REMOVED, toggled.change());
    }

    @Test
    void aFullListTakesNoMore() {
        EmblemSelection.Toggled toggled = EmblemSelection.toggle(List.of("a", "b"), "c", 2);

        assertEquals(List.of("a", "b"), toggled.emblems());
        assertEquals(EmblemSelection.Change.FULL, toggled.change());
    }

    @Test
    void aDraggedEmblemTakesThePlaceOfTheOneItIsDroppedOn() {
        assertEquals(List.of("c", "a", "b"), EmblemSelection.move(List.of("a", "b", "c"), "c", "a"));
        assertEquals(List.of("b", "c", "a"), EmblemSelection.move(List.of("a", "b", "c"), "a", "c"));
    }

    @Test
    void movingToItselfOrToSomethingUnknownChangesNothing() {
        assertEquals(List.of("a", "b"), EmblemSelection.move(List.of("a", "b"), "a", "a"));
        assertEquals(List.of("a", "b"), EmblemSelection.move(List.of("a", "b"), "a", "x"));
    }

    @Test
    void theChoicesOfPlayersAreKeptByTheirPanitaAccount() {
        EmblemSelections selections = EmblemSelections.EMPTY.with(7, List.of("a", "b")).with(9, List.of());

        assertEquals(Optional.of(List.of("a", "b")), selections.of(7));
        assertEquals(Optional.of(List.of()), selections.of(9));
        assertEquals(Optional.empty(), selections.of(1));
        assertEquals(Optional.empty(), selections.without(7).of(7));
        assertEquals(Optional.of(List.of("c")), selections.with(7, List.of("c")).of(7));
    }

    @Test
    void theChoicesSurviveAWriteAndARead() {
        EmblemSelections selections = EmblemSelections.EMPTY.with(7, List.of("a", "b")).with(9, List.of());

        var json = EmblemSelections.CODEC.encodeStart(JsonOps.INSTANCE, selections).getOrThrow();

        assertEquals(selections, EmblemSelections.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
