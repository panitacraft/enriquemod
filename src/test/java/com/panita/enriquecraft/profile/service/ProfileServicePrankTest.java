package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.CachedProfiles;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileServicePrankTest {

    private static final int USER = 7;
    private static final Duration WEEK = Duration.ofDays(7);

    @TempDir
    Path directory;

    private final AtomicReference<EmblemSelections> stored = new AtomicReference<>(EmblemSelections.EMPTY);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-12T10:00:00Z"));
    private ProfileService service;

    @BeforeEach
    void bind() {
        ProfileConfig config = new ConfigManager(directory).bind("profile", ProfileConfig.class);
        service = new ProfileService(new ProfileCache(now::get, () -> CachedProfiles.EMPTY, cached -> { }), new EmblemResolver(config),
                new LinkedPlayers(() -> LinkedSnapshot.EMPTY), stored::get, stored::set, now::get, config.prankCooldownSeconds::get);
    }

    private ProfileService.PrankChange.Status change(String level, boolean bypass) {
        return service.choosePrank(USER, level, bypass).status();
    }

    private Optional<String> shown() {
        return stored.get().prankOf(USER).map(EmblemSelections.Prank::level);
    }

    @Test
    void theFirstChangeHasNoWaitAndIsKeptWithItsTime() {
        assertEquals(ProfileService.PrankChange.Status.CHANGED, change("enrique6_prank_2", false));

        assertEquals(Optional.of(new EmblemSelections.Prank(USER, "enrique6_prank_2", now.get())), stored.get().prankOf(USER));
    }

    @Test
    void aSecondChangeInsideTheWaitIsRefusedAndSaysHowLongIsLeft() {
        change("enrique6_prank_2", false);
        now.set(now.get().plus(Duration.ofDays(2)));

        ProfileService.PrankChange refused = service.choosePrank(USER, "enrique6_prank_3", false);

        assertEquals(ProfileService.PrankChange.Status.COOLDOWN, refused.status());
        assertEquals(Duration.ofDays(5), refused.remaining());
        assertEquals(Optional.of("enrique6_prank_2"), shown());
    }

    @Test
    void afterTheWaitTheLevelCanChangeAgain() {
        change("enrique6_prank_2", false);
        now.set(now.get().plus(WEEK));

        assertEquals(ProfileService.PrankChange.Status.CHANGED, change("enrique6_prank_3", false));
        assertEquals(Optional.of("enrique6_prank_3"), shown());
    }

    @Test
    void theStartingLevelCanNeverBeChosenAgainNotEvenAfterTheWaitOrWithTheBypass() {
        change("enrique6_prank_2", false);
        now.set(now.get().plus(WEEK));

        assertEquals(ProfileService.PrankChange.Status.UNCHANGED, change("enrique6_prank_0", false));
        assertEquals(ProfileService.PrankChange.Status.UNCHANGED, change("enrique6_prank_0", true));
        assertEquals(Optional.of("enrique6_prank_2"), shown());
    }

    @Test
    void thereIsNoZeroPlusLevelToChoose() {
        assertEquals(ProfileService.PrankChange.Status.UNCHANGED, change("enrique6_prank_0_plus", false));
        assertEquals(Optional.empty(), shown());
    }

    @Test
    void whoMayBypassChangesAtOnce() {
        change("enrique6_prank_2", false);

        assertEquals(ProfileService.PrankChange.Status.CHANGED, change("enrique6_prank_3", true));
        assertEquals(Optional.of("enrique6_prank_3"), shown());
    }

    @Test
    void theLevelAPlayerIsAlreadyAtOrOneThatDoesNotExistChangesNothing() {
        assertEquals(ProfileService.PrankChange.Status.UNCHANGED, change("enrique6_prank_0", false));
        assertEquals(ProfileService.PrankChange.Status.UNCHANGED, change("not_a_level", false));
        assertEquals(Optional.empty(), shown());

        change("enrique6_prank_1", false);
        assertEquals(ProfileService.PrankChange.Status.UNCHANGED, change("enrique6_prank_1", false));
    }
}
