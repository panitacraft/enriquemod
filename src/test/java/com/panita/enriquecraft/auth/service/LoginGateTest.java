package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LoginGateTest {

    private final AtomicInteger asked = new AtomicInteger();
    private PremiumLookup.Result answer = new PremiumLookup.Result.Missing();
    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final LoginGate gate = new LoginGate(new AttemptLimiter(() -> now), new PremiumLookup(name -> {
        asked.incrementAndGet();
        return CompletableFuture.completedFuture(answer);
    }, () -> now, () -> Duration.ofMinutes(30), () -> 500));

    private LoginGate.Verdict plan(Optional<Duration> blocked, boolean whitelist, Optional<AuthMode> listed) {
        return gate.plan(blocked, whitelist, listed, "Someone").join();
    }

    @Test
    void aBlockedAddressIsRefusedBeforeAnythingElse() {
        LoginGate.Verdict verdict = plan(Optional.of(Duration.ofMinutes(5)), true, Optional.of(AuthMode.PREMIUM));

        assertEquals(new LoginGate.Verdict.Blocked(Duration.ofMinutes(5)), verdict);
    }

    @Test
    void aListedNameProceedsWithItsModeAndNeverAsksMojang() {
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.CRACKED), plan(Optional.empty(), true, Optional.of(AuthMode.CRACKED)));
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.PREMIUM), plan(Optional.empty(), false, Optional.of(AuthMode.PREMIUM)));
        assertEquals(0, asked.get());
    }

    @Test
    void anUnlistedNameIsRefusedWhileTheWhitelistIsOn() {
        assertInstanceOf(LoginGate.Verdict.NotWhitelisted.class, plan(Optional.empty(), true, Optional.empty()));
        assertEquals(0, asked.get());
    }

    @Test
    void withTheWhitelistOffMojangDecidesTheMode() {
        answer = new PremiumLookup.Result.Found(new NameAndId(UUID.randomUUID(), "Someone"));
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.PREMIUM), plan(Optional.empty(), false, Optional.empty()));

        answer = new PremiumLookup.Result.Missing();
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.CRACKED), gate.plan(Optional.empty(), false, Optional.empty(), "Other").join());
    }

    @Test
    void whenMojangCannotAnswerTheNameIsRefusedNotGuessed() {
        answer = new PremiumLookup.Result.Unavailable();

        assertInstanceOf(LoginGate.Verdict.Unavailable.class, plan(Optional.empty(), false, Optional.empty()));
    }
}
