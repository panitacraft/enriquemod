package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistAdminTest {

    private static final NameAndId PREMIUM = new NameAndId(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360");

    private PremiumLookup.Result answer = new PremiumLookup.Result.Missing();
    private int asked;
    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final WhitelistAdmin admin = new WhitelistAdmin(new PremiumLookup(name -> {
        asked++;
        return CompletableFuture.completedFuture(answer);
    }, () -> now, () -> Duration.ofMinutes(30), () -> 500));

    private static WhitelistAdmin.Resolution entry(NameAndId id) {
        return new WhitelistAdmin.Resolution.Entry(id);
    }

    @Test
    void crackedIsTheOfflineEntryOfTheNameAsTypedAndNeverAsksMojang() {
        assertEquals(entry(NameAndId.createOffline("SujetoDePrueba")), admin.resolve("SujetoDePrueba", Optional.of(AuthMode.CRACKED)));
        assertEquals(0, asked);
    }

    @Test
    void theCapitalizationOfACrackedNameChangesItsUuid() {
        assertNotEquals(admin.resolve("Tezzlar", Optional.of(AuthMode.CRACKED)), admin.resolve("tezzlar", Optional.of(AuthMode.CRACKED)));
    }

    @Test
    void aFoundAccountIsPremiumWithItsRealUuidAndExactSpelling() {
        answer = new PremiumLookup.Result.Found(PREMIUM);

        assertEquals(entry(PREMIUM), admin.resolve("javithor360", Optional.empty()));
        assertEquals(entry(PREMIUM), admin.resolve("javithor360", Optional.of(AuthMode.PREMIUM)));
    }

    @Test
    void aNameNobodyOwnsIsCrackedWhenDetectedAndAnErrorWhenPremiumWasAsked() {
        assertEquals(entry(NameAndId.createOffline("Tezzlar")), admin.resolve("Tezzlar", Optional.empty()));
        assertEquals(new WhitelistAdmin.Resolution.NotPremium("Tezzlar"), admin.resolve("Tezzlar", Optional.of(AuthMode.PREMIUM)));
    }

    @Test
    void whenMojangCannotAnswerNothingIsGuessedForPremiumOrDetection() {
        answer = new PremiumLookup.Result.Unavailable();

        assertEquals(new WhitelistAdmin.Resolution.LookupFailed("Tezzlar"), admin.resolve("Tezzlar", Optional.empty()));
        assertEquals(new WhitelistAdmin.Resolution.LookupFailed("Tezzlar"), admin.resolve("Tezzlar", Optional.of(AuthMode.PREMIUM)));
    }

    @Test
    void whenMojangCannotAnswerCrackedStillWorksBecauseStaffSaidSo() {
        answer = new PremiumLookup.Result.Unavailable();

        assertEquals(entry(NameAndId.createOffline("Tezzlar")), admin.resolve("Tezzlar", Optional.of(AuthMode.CRACKED)));
    }

    @Test
    void namesThatCannotBelongToAPlayerAreRejectedBeforeAnythingElse() {
        assertEquals(new WhitelistAdmin.Resolution.InvalidName("no es valido"), admin.resolve("no es valido", Optional.empty()));
        assertEquals(new WhitelistAdmin.Resolution.InvalidName(""), admin.resolve("", Optional.of(AuthMode.CRACKED)));
        assertEquals(new WhitelistAdmin.Resolution.InvalidName("ñandú"), admin.resolve("ñandú", Optional.of(AuthMode.CRACKED)));
        assertEquals(new WhitelistAdmin.Resolution.InvalidName("a".repeat(17)), admin.resolve("a".repeat(17), Optional.empty()));
        assertEquals(0, asked);
    }

    @Test
    void addingAnUnknownPlayerAddsIt() {
        WhitelistAdmin.Plan plan = WhitelistAdmin.plan(List.of(PREMIUM), NameAndId.createOffline("Tezzlar"));

        assertTrue(plan.add());
        assertTrue(plan.replaced().isEmpty());
        assertEquals(WhitelistAdmin.Change.ADDED, plan.change());
    }

    @Test
    void addingTheSameEntryAgainChangesNothing() {
        WhitelistAdmin.Plan plan = WhitelistAdmin.plan(List.of(PREMIUM), PREMIUM);

        assertEquals(WhitelistAdmin.Change.UNCHANGED, plan.change());
    }

    @Test
    void anotherModeForTheSameNameReplacesTheOldEntryIgnoringCase() {
        NameAndId crackedJavi = NameAndId.createOffline("javithor360");

        WhitelistAdmin.Plan plan = WhitelistAdmin.plan(List.of(crackedJavi), PREMIUM);

        assertEquals(WhitelistAdmin.Change.REPLACED, plan.change());
        assertEquals(List.of(crackedJavi), plan.replaced());
    }
}
