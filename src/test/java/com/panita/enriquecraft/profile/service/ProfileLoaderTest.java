package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.EmblemApi;
import com.panita.enriquecraft.profile.client.ModProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileLoaderTest {

    private static final ModProfile PROFILE = new ModProfile(7, Optional.of("Alex"), "alex", List.of());

    private final UUID player = UUID.randomUUID();
    private final ProfileCache cache = new ProfileCache();
    private final LinkedPlayers linked = new LinkedPlayers(() -> new LinkedSnapshot(
            List.of(new LinkedPlayer(7, "Alex", AuthMode.PREMIUM, "1")), Optional.empty()));
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
        ProfileLoader.Outcome outcome = loader.load(player, "alex").join();

        assertInstanceOf(ProfileLoader.Outcome.Updated.class, outcome);
        assertEquals(Optional.of(PROFILE), cache.get(player));
        assertEquals(7, askedAccount);
        assertEquals("enriquecraft6", askedEdition);
    }

    @Test
    void aPlayerWhoIsNotLinkedIsNotAskedAboutAndKeepsNoData() {
        cache.put(player, PROFILE);

        ProfileLoader.Outcome outcome = loader.load(player, "Stranger").join();

        assertInstanceOf(ProfileLoader.Outcome.NotLinked.class, outcome);
        assertEquals(0, asked);
        assertEquals(Optional.empty(), cache.get(player));
    }

    @Test
    void anAccountTheApiDoesNotKnowLosesItsData() {
        cache.put(player, PROFILE);
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.NotFound());

        assertInstanceOf(ProfileLoader.Outcome.NotLinked.class, loader.load(player, "Alex").join());
        assertEquals(Optional.empty(), cache.get(player));
    }

    @Test
    void aFailureKeepsWhatTheCacheHad() {
        cache.put(player, PROFILE);
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.Failed("the API answered with status 500"));

        ProfileLoader.Outcome outcome = loader.load(player, "Alex").join();

        assertEquals(new ProfileLoader.Outcome.Failed("the API answered with status 500"), outcome);
        assertEquals(Optional.of(PROFILE), cache.get(player));
    }

    @Test
    void anUnexpectedErrorIsAFailureAndNeverEscapes() {
        answer = CompletableFuture.failedFuture(new IllegalStateException("boom"));

        ProfileLoader.Outcome outcome = loader.load(player, "Alex").join();

        assertTrue(assertInstanceOf(ProfileLoader.Outcome.Failed.class, outcome).reason().contains("IllegalStateException"));
        assertInstanceOf(ProfileLoader.Outcome.Updated.class, retry());
    }

    private ProfileLoader.Outcome retry() {
        answer = CompletableFuture.completedFuture(new EmblemApi.Result.Found(PROFILE));
        return loader.load(player, "Alex").join();
    }

    @Test
    void aPlayerIsOnlyReadOnceAtATime() {
        CompletableFuture<EmblemApi.Result> pending = new CompletableFuture<>();
        answer = pending;

        CompletableFuture<ProfileLoader.Outcome> first = loader.load(player, "Alex");
        ProfileLoader.Outcome second = loader.load(player, "Alex").join();
        pending.complete(new EmblemApi.Result.Found(PROFILE));

        assertInstanceOf(ProfileLoader.Outcome.Busy.class, second);
        assertInstanceOf(ProfileLoader.Outcome.Updated.class, first.join());
        assertEquals(1, asked);
        assertInstanceOf(ProfileLoader.Outcome.Updated.class, loader.load(player, "Alex").join());
    }
}
