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

    private static final NameAndId PREMIUM = new NameAndId(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360");
    private static final NameAndId CRACKED = NameAndId.createOffline("Tezzlar");

    private final AtomicInteger asked = new AtomicInteger();
    private PremiumLookup.Result answer = new PremiumLookup.Result.Missing();
    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final LoginGate gate = new LoginGate(new AttemptLimiter(() -> now), new PremiumLookup(name -> {
        asked.incrementAndGet();
        return CompletableFuture.completedFuture(answer);
    }, () -> now, () -> Duration.ofMinutes(30), () -> 500));

    private LoginGate.Verdict plan(Optional<Duration> blocked, boolean whitelist, Optional<NameAndId> listed) {
        return plan(blocked, whitelist, false, listed);
    }

    private LoginGate.Verdict plan(Optional<Duration> blocked, boolean whitelist, boolean online, Optional<NameAndId> listed) {
        return gate.plan(blocked, whitelist, online, listed, "typed").join();
    }

    @Test
    void aBlockedAddressIsRefusedBeforeAnythingElse() {
        LoginGate.Verdict verdict = plan(Optional.of(Duration.ofMinutes(5)), true, Optional.of(PREMIUM));

        assertEquals(new LoginGate.Verdict.Blocked(Duration.ofMinutes(5)), verdict);
    }

    @Test
    void aListedNameProceedsWithItsModeAndSpellingAndNeverAsksMojang() {
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.CRACKED, "Tezzlar"), plan(Optional.empty(), true, Optional.of(CRACKED)));
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.PREMIUM, "Javithor360"), plan(Optional.empty(), false, Optional.of(PREMIUM)));
        assertEquals(0, asked.get());
    }

    @Test
    void aPinNameThatIsAlreadyOnlineCannotBeTakenOver() {
        assertInstanceOf(LoginGate.Verdict.AlreadyOnline.class, plan(Optional.empty(), true, true, Optional.of(CRACKED)));
    }

    @Test
    void aPremiumNameThatIsAlreadyOnlineIsReplacedBecauseTheOwnerProvedThemselves() {
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.PREMIUM, "Javithor360"), plan(Optional.empty(), true, true, Optional.of(PREMIUM)));
    }

    @Test
    void anUnknownNameThatIsAlreadyOnlineAndHasNoAccountCannotBeTakenOverEither() {
        answer = new PremiumLookup.Result.Missing();

        assertInstanceOf(LoginGate.Verdict.AlreadyOnline.class,
                gate.plan(Optional.empty(), false, true, Optional.empty(), "Other").join());
    }

    @Test
    void anUnlistedNameIsRefusedWhileTheWhitelistIsOn() {
        assertInstanceOf(LoginGate.Verdict.NotWhitelisted.class, plan(Optional.empty(), true, Optional.empty()));
        assertEquals(0, asked.get());
    }

    @Test
    void withTheWhitelistOffMojangDecidesTheModeAndTheSpelling() {
        answer = new PremiumLookup.Result.Found(PREMIUM);
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.PREMIUM, "Javithor360"), gate.plan(Optional.empty(), false, false, Optional.empty(), "javithor360").join());

        answer = new PremiumLookup.Result.Missing();
        assertEquals(new LoginGate.Verdict.Proceed(AuthMode.CRACKED, "Other"), gate.plan(Optional.empty(), false, false, Optional.empty(), "Other").join());
    }

    @Test
    void whenMojangCannotAnswerTheNameIsRefusedNotGuessed() {
        answer = new PremiumLookup.Result.Unavailable();

        assertInstanceOf(LoginGate.Verdict.Unavailable.class, plan(Optional.empty(), false, Optional.empty()));
    }
}
