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
    void choosingTheEmblemOfThePlaceEmptiesIt() {
        assertEquals(List.of("a", "", "c"), EmblemSelection.place(List.of("a", "b", "c"), 1, "b", 3));
    }

    @Test
    void anEmblemShownElsewhereTradesPlaces() {
        assertEquals(List.of("c", "b", "a"), EmblemSelection.place(List.of("a", "b", "c"), 0, "c", 3));
    }

    @Test
    void anEmblemCanStandAloneInTheLastPlace() {
        assertEquals(List.of("", "", "x"), EmblemSelection.place(List.of("", "", ""), 2, "x", 3));
        assertEquals(List.of("", "", "x"), EmblemSelection.place(List.of(), 2, "x", 3));
    }

    @Test
    void movingAnEmblemToAnEmptyPlaceLeavesItsOldPlaceEmpty() {
        assertEquals(List.of("", "", "a"), EmblemSelection.place(List.of("a"), 2, "a", 3));
    }

    @Test
    void theEmptyPlacesAtTheEndAreNotKept() {
        assertEquals(List.of(), EmblemSelection.place(List.of("a"), 0, "a", 3));
        assertEquals(List.of("a"), EmblemSelection.place(List.of("a", "b"), 1, "b", 3));
    }
}
