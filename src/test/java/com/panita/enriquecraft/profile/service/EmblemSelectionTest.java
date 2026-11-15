package com.panita.enriquecraft.profile.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmblemSelectionTest {

    @Test
    void anEmblemReplacesTheOneInThePlaceItIsChosenFor() {
        assertEquals(List.of("a", "x", "c"), EmblemSelection.place(List.of("a", "b", "c"), 1, "x", 3));
    }

    @Test
    void choosingTheEmblemOfThePlaceGivesItUp() {
        assertEquals(List.of("a", "c"), EmblemSelection.place(List.of("a", "b", "c"), 1, "b", 3));
    }

    @Test
    void anEmblemShownElsewhereTradesPlaces() {
        assertEquals(List.of("c", "b", "a"), EmblemSelection.place(List.of("a", "b", "c"), 0, "c", 3));
    }

    @Test
    void anEmptyPlaceTakesANewEmblemAtTheEnd() {
        assertEquals(List.of("a", "x"), EmblemSelection.place(List.of("a"), 2, "x", 3));
    }

    @Test
    void anEmptyPlaceMovesAnEmblemShownElsewhereToTheEnd() {
        assertEquals(List.of("b", "a"), EmblemSelection.place(List.of("a", "b"), 2, "a", 3));
    }

    @Test
    void aFullListIsNotGrownByAnEmptyPlace() {
        assertEquals(List.of("a", "b"), EmblemSelection.place(List.of("a", "b"), 2, "x", 2));
    }
}
