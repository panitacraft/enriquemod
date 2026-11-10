package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.EmblemApi;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.data.CachedProfiles;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileLoaderTest {

    private static final ModProfile PROFILE = new ModProfile(7, Optional.of("Alex"), "alex", List.of());
    private static final ProfileLoader.Freshness FORCE = new ProfileLoader.Freshness.Force();
    private static final ProfileLoader.Freshness TEN_MINUTES = new ProfileLoader.Freshness.AllowCached(Duration.ofMinutes(10));

    private Instant now = Instant.parse("2026-11-09T12:00:00Z");
    private final UUID player = UUID.randomUUID();
    private CachedProfiles saved = CachedProfiles.EMPTY;
    private final ProfileCache cache = new ProfileCache(() -> now, () -> saved, value -> saved = value);
    private final LinkedPlayers linked = new LinkedPlayers(() -> new LinkedSnapshot(
            List.of(new LinkedPlayer(7, "Alex", Optional.of(AuthMode.PREMIUM), "1")), Optional.empty()));
    private CompletableFuture<EmblemApi.Result> answer = CompletableFuture.completedFuture(new EmblemApi.Result.Found(PROFILE));
    private int asked;
    private String askedEdition;
    private int askedAccount;

    private final ProfileLoader loader = new ProfileLoader((userId, editionId) -> {
        asked++;
        askedAccount = userId;
        askedEdition = editionId;
        return answer;
    }, linked, cache, () -> "enriquecraft6");

    @Test
    void aLinkedPlayerIsReadByTheirPanitaAccountAndTheEditionOfTheServer() {
        ProfileLoader.Outcome outcome = loader.load(player, "alex", FORCE).join();

        assertInstanceOf(ProfileLoader.Outcome.Updated.class, outcome);
        assertEquals(Optional.of(PROFILE), cache.get(player));
        assertEquals(7, askedAccount);
        assertEquals("enriquecraft6", askedEdition);
    }

    @Test
    void aRecentAnswerIsServedFromTheCacheWithoutAskingTheApi() {
        loader.load(player, "Alex", TEN_MINUTES).join();
        now = now.plus(Duration.ofMinutes(9));

        assertInstanceOf(ProfileLoader.Outcome.Current.class, loader.load(player, "Alex", TEN_MINUTES).join());
        assertEquals(1, asked);
        assertEquals(Optional.of(PROFILE), cache.get(player));
    }

    @Test
    void aRestartDoesNotAskTheApiAgain() {
        loader.load(player, "Alex", TEN_MINUTES).join();
        now = now.plus(Duration.ofMinutes(2));
        // A new cache over what was saved stands for the server after a restart.
        ProfileCache restarted = new ProfileCache(() -> now, () -> saved, value -> saved = value);
        ProfileLoader afterRestart = new ProfileLoader((userId, editionId) -> {
            asked++;
            return answer;
        }, linked, restarted, () -> "enriquecraft6");

        assertInstanceOf(ProfileLoader.Outcome.Current.class, afterRestart.load(player, "Alex", TEN_MINUTES).join());
        assertEquals(1, asked);
        assertEquals(Optional.of(PROFILE), restarted.get(player));
    }

    @Test
    void anOldAnswerIsReadAgain() {
        loader.load(player, "Alex", TEN_MINUTES).join();
        now = now.plus(Duration.ofMinutes(10));

        assertInstanceOf(ProfileLoader.Outcome.Updated.class, loader.load(player, "Alex", TEN_MINUTES).join());
        assertEquals(2, asked);
    }

    @Test
    void aForcedReadIgnoresTheCache() {
        loader.load(player, "Alex", TEN_MINUTES).join();

        assertInstanceOf(ProfileLoader.Outcome.Updated.class, loader.load(player, "Alex", FORCE).join());
        assertEquals(2, asked);
    }

    @Test
    void aFailureIsNotRememberedAsAnAnswer() {
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.Failed("the API answered with status 500"));
        loader.load(player, "Alex", TEN_MINUTES).join();

        loader.load(player, "Alex", TEN_MINUTES).join();

        assertEquals(2, asked);
    }

    @Test
    void anAccountTheApiDoesNotKnowIsNotAskedAboutAgainEither() {
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.NotFound());
        loader.load(player, "Alex", TEN_MINUTES).join();

        assertInstanceOf(ProfileLoader.Outcome.Current.class, loader.load(player, "Alex", TEN_MINUTES).join());
        assertEquals(1, asked);
    }

    @Test
    void aPlayerWhoIsNotLinkedIsNotAskedAboutAndKeepsNoData() {
        cache.put(player, PROFILE);

        ProfileLoader.Outcome outcome = loader.load(player, "Stranger", FORCE).join();

        assertInstanceOf(ProfileLoader.Outcome.NotLinked.class, outcome);
        assertEquals(0, asked);
        assertEquals(Optional.empty(), cache.get(player));
    }

    @Test
    void anAccountTheApiDoesNotKnowLosesItsData() {
        cache.put(player, PROFILE);
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.NotFound());

        assertInstanceOf(ProfileLoader.Outcome.NotLinked.class, loader.load(player, "Alex", FORCE).join());
        assertEquals(Optional.empty(), cache.get(player));
    }

    @Test
    void aFailureKeepsWhatTheCacheHad() {
        cache.put(player, PROFILE);
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.Failed("the API answered with status 500"));

        ProfileLoader.Outcome outcome = loader.load(player, "Alex", FORCE).join();

        assertEquals(new ProfileLoader.Outcome.Failed("the API answered with status 500"), outcome);
        assertEquals(Optional.of(PROFILE), cache.get(player));
    }

    @Test
    void anUnexpectedErrorIsAFailureAndNeverEscapes() {
        answer = CompletableFuture.failedFuture(new IllegalStateException("boom"));

        ProfileLoader.Outcome outcome = loader.load(player, "Alex", FORCE).join();

        assertTrue(assertInstanceOf(ProfileLoader.Outcome.Failed.class, outcome).reason().contains("IllegalStateException"));
        assertInstanceOf(ProfileLoader.Outcome.Updated.class, retry());
    }

    private ProfileLoader.Outcome retry() {
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.Found(PROFILE));
        return loader.load(player, "Alex", FORCE).join();
    }

    @Test
    void aPlayerIsOnlyReadOnceAtATime() {
        CompletableFuture<EmblemApi.Result> pending = new CompletableFuture<>();
        answer = pending;

        CompletableFuture<ProfileLoader.Outcome> first = loader.load(player, "Alex", FORCE);
        ProfileLoader.Outcome second = loader.load(player, "Alex", FORCE).join();
        pending.complete(new EmblemApi.Result.Found(PROFILE));

        assertInstanceOf(ProfileLoader.Outcome.Busy.class, second);
        assertInstanceOf(ProfileLoader.Outcome.Updated.class, first.join());
        assertEquals(1, asked);
        assertInstanceOf(ProfileLoader.Outcome.Updated.class, loader.load(player, "Alex", FORCE).join());
    }
}
