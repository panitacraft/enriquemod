package com.panita.enriquecraft.chat.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplyTrackerTest {

    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID EVA = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    private final ReplyTracker tracker = new ReplyTracker();

    @Test
    void nobodyHasAContactAtFirst() {
        assertEquals(Optional.empty(), tracker.lastContactOf(ANA));
    }

    @Test
    void bothSidesOfAMessageCanAnswer() {
        tracker.exchanged(ANA, BOB);

        assertEquals(Optional.of(BOB), tracker.lastContactOf(ANA));
        assertEquals(Optional.of(ANA), tracker.lastContactOf(BOB));
    }

    @Test
    void theLastContactWins() {
        tracker.exchanged(ANA, BOB);
        tracker.exchanged(EVA, ANA);

        assertEquals(Optional.of(EVA), tracker.lastContactOf(ANA));
        assertEquals(Optional.of(ANA), tracker.lastContactOf(EVA));
        assertEquals(Optional.of(ANA), tracker.lastContactOf(BOB));
    }

    @Test
    void leavingForgetsThePlayerAndEveryoneWhoWouldAnswerThem() {
        tracker.exchanged(ANA, BOB);

        tracker.forget(ANA);

        assertEquals(Optional.empty(), tracker.lastContactOf(ANA));
        assertEquals(Optional.empty(), tracker.lastContactOf(BOB));
    }
}
