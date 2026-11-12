package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.CachedProfiles;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The emblems of a linked player found by name, which is all a Discord message says of its author. */
class ProfileServiceAccountTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    @TempDir
    Path directory;

    private final AtomicReference<CachedProfiles> cached = new AtomicReference<>(CachedProfiles.EMPTY);
    private final AtomicReference<EmblemSelections> selections = new AtomicReference<>(EmblemSelections.EMPTY);
    private ProfileCache cache;
    private ProfileService service;

    private static ApiEmblem emblem(String id, int position) {
        return new ApiEmblem(id, "Titulo " + id, Optional.empty(), Optional.empty(), EmblemCategory.EMBLEM, position, Optional.of("enriquecraft6"));
    }

    @BeforeEach
    void bind() {
        ProfileConfig config = new ConfigManager(directory).bind("profile", ProfileConfig.class);
        cache = new ProfileCache(() -> NOW, cached::get, cached::set);
        LinkedSnapshot linked = new LinkedSnapshot(List.of(new LinkedPlayer(7, "Alex", Optional.empty(), "111")), Optional.empty());
        service = new ProfileService(cache, new EmblemResolver(config), new LinkedPlayers(() -> linked), selections::get, selections::set,
                () -> NOW, config.prankCooldownSeconds::get);
    }

    private static List<String> ids(List<EmblemView> views) {
        return views.stream().map(EmblemView::id).toList();
    }

    @Test
    void aLinkedPlayerHasTheEmblemsAndThePrankTheyChoseWhetherOrNotTheyAreOnline() {
        cache.put(UUID.randomUUID(), new ModProfile(7, Optional.of("Alex"), "alex", List.of(emblem("a", 1), emblem("b", 2), emblem("c", 3))));
        selections.set(EmblemSelections.EMPTY.with(7, List.of("c", "a")).withPrank(7, "enrique6_prank_2", NOW));

        PlayerEmblems emblems = service.emblemsOf("aLEX");

        assertEquals("enrique6_prank_2", emblems.prank().id());
        assertEquals(List.of("c", "a"), ids(emblems.chosen()));
        assertEquals(1, emblems.empty().size());
    }

    @Test
    void aLinkedPlayerTheApiHasToldNothingOfHasTheirPrankAndEmptyPlaces() {
        selections.set(EmblemSelections.EMPTY.withPrank(7, "enrique6_prank_1", NOW));

        PlayerEmblems emblems = service.emblemsOf("Alex");

        assertEquals("enrique6_prank_1", emblems.prank().id());
        assertEquals(List.of(), emblems.chosen());
        assertEquals(3, emblems.empty().size());
    }

    @Test
    void aNameNotOnTheListHasTheFirstPrankAndThreeEmptyPlaces() {
        PlayerEmblems emblems = service.emblemsOf("Stranger");

        assertEquals("enrique6_prank_0", emblems.prank().id());
        assertEquals(List.of(), emblems.chosen());
        assertEquals(3, emblems.empty().size());
    }

    @Test
    void anotherAccountsEmblemsAreNeverShown() {
        cache.put(UUID.randomUUID(), new ModProfile(99, Optional.of("Sam"), "sam", List.of(emblem("secret", 1))));

        assertEquals(List.of(), ids(service.emblemsOf("Alex").chosen()));
    }
}
