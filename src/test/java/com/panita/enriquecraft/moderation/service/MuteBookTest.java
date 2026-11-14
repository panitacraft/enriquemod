package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.moderation.data.MuteRecord;
import com.panita.enriquecraft.moderation.data.Mutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuteBookTest {

    private static final UUID TEZZLAR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant START = Instant.parse("2026-11-14T12:00:00Z");

    private Mutes stored;
    private Instant now;
    private MuteBook book;

    @BeforeEach
    void create() {
        stored = Mutes.EMPTY;
        now = START;
        book = new MuteBook(() -> stored, mutes -> stored = mutes, () -> now);
    }

    @Test
    void aPermanentMuteNeverEnds() {
        book.mute(TEZZLAR, "Tezzlar", Optional.empty(), "Admin", Optional.of("spam"));
        now = START.plus(Duration.ofDays(3650));

        MuteRecord record = book.activeFor(TEZZLAR).orElseThrow();
        assertEquals(Optional.empty(), record.expires());
        assertEquals(Optional.of("spam"), record.reason());
        assertEquals("Admin", record.source());
    }

    @Test
    void aTimedMuteEndsWhenItsTimeIsUp() {
        book.mute(TEZZLAR, "Tezzlar", Optional.of(Duration.ofMinutes(10)), "Admin", Optional.empty());

        now = START.plus(Duration.ofMinutes(9));
        assertEquals(Optional.of(Duration.ofMinutes(1)), book.activeFor(TEZZLAR).orElseThrow().remainingAt(now));

        now = START.plus(Duration.ofMinutes(10));
        assertEquals(Optional.empty(), book.activeFor(TEZZLAR));
        assertEquals(List.of(), book.mutedNames());
    }

    @Test
    void mutingAMutedPlayerChangesNothing() {
        book.mute(TEZZLAR, "Tezzlar", Optional.empty(), "Admin", Optional.of("first"));
        Mutes before = stored;

        assertEquals(Optional.empty(), book.mute(TEZZLAR, "Tezzlar", Optional.empty(), "Other", Optional.of("second")));
        assertEquals(before, stored);
    }

    @Test
    void aPlayerWhoseMuteEndedCanBeMutedAgainAndTheOldEntryIsReplaced() {
        book.mute(TEZZLAR, "Tezzlar", Optional.of(Duration.ofMinutes(1)), "Admin", Optional.empty());
        now = START.plus(Duration.ofMinutes(5));

        assertTrue(book.mute(TEZZLAR, "Tezzlar", Optional.empty(), "Admin", Optional.of("again")).isPresent());
        assertEquals(1, stored.entries().size());
        assertEquals(Optional.of("again"), book.activeFor(TEZZLAR).orElseThrow().reason());
    }

    @Test
    void unmutingLiftsTheMuteOfThatNameWhateverItsCase() {
        book.mute(TEZZLAR, "Tezzlar", Optional.empty(), "Admin", Optional.empty());
        book.mute(OTHER, "Other", Optional.empty(), "Admin", Optional.empty());

        List<MuteRecord> lifted = book.unmute("tezzlar");

        assertEquals(List.of(TEZZLAR), lifted.stream().map(MuteRecord::id).toList());
        assertEquals(Optional.empty(), book.activeFor(TEZZLAR));
        assertTrue(book.activeFor(OTHER).isPresent());
        assertEquals(List.of("Other"), book.mutedNames());
    }

    @Test
    void unmutingSomeoneWhoIsNotMutedLiftsNothing() {
        book.mute(TEZZLAR, "Tezzlar", Optional.of(Duration.ofMinutes(1)), "Admin", Optional.empty());
        now = START.plus(Duration.ofMinutes(5));

        assertEquals(List.of(), book.unmute("Tezzlar"));
        assertEquals(List.of(), book.unmute("Nobody"));
    }

    @Test
    void theRecordSurvivesItsCodec() {
        MuteRecord record = new MuteRecord(TEZZLAR, "Tezzlar", START, Optional.of(START.plusSeconds(60)), "Admin", Optional.of("spam"));
        Mutes mutes = new Mutes(List.of(record, new MuteRecord(OTHER, "Other", START, Optional.empty(), "Console", Optional.empty())));

        var encoded = Mutes.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, mutes).getOrThrow();
        assertEquals(mutes, Mutes.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow());
    }
}
