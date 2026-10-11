package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.auth.data.HeldState;
import com.panita.enriquecraft.core.framework.data.WorldData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path directory;

    private Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private AccountService service;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        WorldData worldData = new WorldData();
        worldData.attach(directory, NbtOps.INSTANCE);
        service = new AccountService(worldData, new PinHasher(new byte[32]), () -> now);
    }

    @Test
    void aPinIsVerifiedAndItsLengthIsRemembered() {
        service.setPin(PLAYER, "Tezzlar", "123456");

        assertTrue(service.verify(PLAYER, "123456"));
        assertFalse(service.verify(PLAYER, "123457"));
        assertEquals(6, service.pinLength(PLAYER).orElseThrow());
    }

    @Test
    void anUnknownPlayerHasNoPinAndNothingVerifies() {
        assertTrue(service.pinLength(PLAYER).isEmpty());
        assertFalse(service.verify(PLAYER, "1234"));
    }

    @Test
    void aSessionIsValidForTheSameAddressUntilItEnds() {
        service.setPin(PLAYER, "Tezzlar", "1234");
        service.startSession(PLAYER, "10.0.0.1", Duration.ofHours(24));

        assertTrue(service.hasValidSession(PLAYER, "10.0.0.1"));
        assertFalse(service.hasValidSession(PLAYER, "10.0.0.2"));
        now = now.plus(Duration.ofHours(24)).plusSeconds(1);
        assertFalse(service.hasValidSession(PLAYER, "10.0.0.1"));
    }

    @Test
    void theSessionCountsFromThePinEntryNotFromLaterJoins() {
        service.setPin(PLAYER, "Tezzlar", "1234");
        service.startSession(PLAYER, "10.0.0.1", Duration.ofHours(24));
        now = now.plus(Duration.ofHours(23));

        assertTrue(service.hasValidSession(PLAYER, "10.0.0.1"));
        now = now.plus(Duration.ofHours(2));
        assertFalse(service.hasValidSession(PLAYER, "10.0.0.1"));
    }

    @Test
    void endingASessionAndResettingForgetThem() {
        service.setPin(PLAYER, "Tezzlar", "1234");
        service.startSession(PLAYER, "10.0.0.1", Duration.ofHours(24));

        assertTrue(service.endSession(PLAYER));
        assertFalse(service.hasValidSession(PLAYER, "10.0.0.1"));

        service.startSession(PLAYER, "10.0.0.1", Duration.ofHours(24));
        assertTrue(service.reset(PLAYER));
        assertFalse(service.hasValidSession(PLAYER, "10.0.0.1"));
        assertTrue(service.pinLength(PLAYER).isEmpty());
    }

    @Test
    void choosingANewPinEndsTheOldSession() {
        service.setPin(PLAYER, "Tezzlar", "1234");
        service.startSession(PLAYER, "10.0.0.1", Duration.ofHours(24));

        service.setPin(PLAYER, "Tezzlar", "5678");

        assertFalse(service.hasValidSession(PLAYER, "10.0.0.1"));
        assertTrue(service.verify(PLAYER, "5678"));
    }

    @Test
    void operationsOnUnknownPlayersReportThatThereIsNoAccount() {
        assertFalse(service.reset(PLAYER));
        assertFalse(service.endSession(PLAYER));
    }

    @Test
    void theHeldStateIsKeptUntilCleared() {
        HeldState position = new HeldState(Level.NETHER, 1.5, 64, -3.25, 90F, 10F, Optional.of(GameType.SURVIVAL));

        service.holdAt(PLAYER, "Tezzlar", position);

        assertEquals(position, service.heldAt(PLAYER).orElseThrow());
        service.clearHeldAt(PLAYER);
        assertTrue(service.heldAt(PLAYER).isEmpty());
    }

    @Test
    void accountsSurviveARestartAndFindByNameIgnoresCase() {
        service.setPin(PLAYER, "Tezzlar", "1234");
        service.startSession(PLAYER, "10.0.0.1", Duration.ofHours(24));

        WorldData reloaded = new WorldData();
        AccountService restarted = new AccountService(reloaded, new PinHasher(new byte[32]), () -> now);
        reloaded.attach(directory, NbtOps.INSTANCE);

        assertTrue(restarted.verify(PLAYER, "1234"));
        assertTrue(restarted.hasValidSession(PLAYER, "10.0.0.1"));
        assertEquals(PLAYER, restarted.findByName("tezzlar").orElseThrow().id());
    }

    @Test
    void theFileHoldsNeitherThePinNorTheAddress() throws IOException {
        service.setPin(PLAYER, "Tezzlar", "987654");
        service.startSession(PLAYER, "203.0.113.7", Duration.ofHours(24));

        String text = Files.readString(directory.resolve("auth/accounts.snbt"));

        assertFalse(text.contains("987654"));
        assertFalse(text.contains("203.0.113.7"));
    }
}
