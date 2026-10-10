package com.panita.enriquecraft.auth.service;

import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PremiumLookupTest {

    private static final NameAndId JAVI = new NameAndId(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360");

    private Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final AtomicInteger calls = new AtomicInteger();
    private PremiumLookup.Result answer = new PremiumLookup.Result.Found(JAVI);
    private int budget = 500;

    private final PremiumLookup lookup = new PremiumLookup(name -> {
        calls.incrementAndGet();
        return CompletableFuture.completedFuture(answer);
    }, () -> now, () -> Duration.ofMinutes(30), () -> budget);

    @Test
    void parsesAFoundProfileWithItsRealUuid() {
        PremiumLookup.Result result = PremiumLookup.parse(200,
                "{\"id\":\"dd12be4252a94a91a8a111c01849e498\",\"name\":\"Javithor360\"}");

        assertEquals(new PremiumLookup.Result.Found(JAVI), result);
    }

    @Test
    void anUnknownNameIsMissing() {
        assertInstanceOf(PremiumLookup.Result.Missing.class, PremiumLookup.parse(404, "{\"errorMessage\":\"no profile\"}"));
        assertInstanceOf(PremiumLookup.Result.Missing.class, PremiumLookup.parse(204, ""));
    }

    @Test
    void anythingElseIsUnavailableNeverMissing() {
        assertInstanceOf(PremiumLookup.Result.Unavailable.class, PremiumLookup.parse(429, ""));
        assertInstanceOf(PremiumLookup.Result.Unavailable.class, PremiumLookup.parse(500, ""));
        assertInstanceOf(PremiumLookup.Result.Unavailable.class, PremiumLookup.parse(200, "not json"));
        assertInstanceOf(PremiumLookup.Result.Unavailable.class, PremiumLookup.parse(200, "{\"id\":\"short\",\"name\":\"X\"}"));
    }

    @Test
    void answersAreRememberedIgnoringCaseUntilTheyExpire() {
        lookup.lookup("Javithor360").join();
        lookup.lookup("javithor360").join();

        assertEquals(1, calls.get());
        now = now.plus(Duration.ofMinutes(31));
        lookup.lookup("Javithor360").join();
        assertEquals(2, calls.get());
    }

    @Test
    void anUnavailableAnswerIsNotRemembered() {
        answer = new PremiumLookup.Result.Unavailable();
        lookup.lookup("Someone").join();
        lookup.lookup("Someone").join();

        assertEquals(2, calls.get());
    }

    @Test
    void afterTheBudgetIsSpentNothingIsGuessed() {
        budget = 2;
        lookup.lookup("a").join();
        lookup.lookup("b").join();

        assertInstanceOf(PremiumLookup.Result.Unavailable.class, lookup.lookup("c").join());
        assertEquals(2, calls.get());
        now = now.plus(Duration.ofMinutes(11));
        assertInstanceOf(PremiumLookup.Result.Found.class, lookup.lookup("c").join());
    }

    @Test
    void namesNoAccountCanHaveAreMissingWithoutAskingAnyone() {
        for (String name : new String[]{"../profile", "a?b", "a/b", "ñandú", "", "a".repeat(17), "with space"}) {
            assertInstanceOf(PremiumLookup.Result.Missing.class, lookup.lookup(name).join(), name);
        }

        assertEquals(0, calls.get());
    }

    @Test
    void theCacheDoesNotGrowWithoutBound() {
        budget = 100_000;
        for (int name = 0; name < 2500; name++) {
            lookup.lookup("name" + name).join();
        }
        int askedBefore = calls.get();

        // Everything is still fresh, yet the cache was emptied rather than left to grow: early names are asked again.
        lookup.lookup("name0").join();

        assertEquals(askedBefore + 1, calls.get());
    }

    @Test
    void aFailingFetchIsUnavailable() {
        PremiumLookup failing = new PremiumLookup(name -> CompletableFuture.failedFuture(new RuntimeException("down")),
                () -> now, () -> Duration.ofMinutes(30), () -> 500);

        assertInstanceOf(PremiumLookup.Result.Unavailable.class, failing.lookup("x").join());
    }
}
