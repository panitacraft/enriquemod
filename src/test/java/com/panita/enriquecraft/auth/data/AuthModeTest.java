package com.panita.enriquecraft.auth.data;

import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthModeTest {

    @Test
    void theOfflineUuidOfTheNameIsCracked() {
        assertEquals(AuthMode.CRACKED, AuthMode.of(NameAndId.createOffline("Tezzlar")));
    }

    @Test
    void anyOtherUuidIsPremium() {
        NameAndId premium = new NameAndId(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360");

        assertEquals(AuthMode.PREMIUM, AuthMode.of(premium));
    }

    @Test
    void theOfflineUuidOfAnotherNameIsNotCrackedForThisOne() {
        UUID other = NameAndId.createOffline("Someone").id();

        assertEquals(AuthMode.PREMIUM, AuthMode.of(other, "Tezzlar"));
    }

    @Test
    void idsFindTheirMode() {
        assertEquals(AuthMode.CRACKED, AuthMode.byId("cracked").orElseThrow());
        assertTrue(AuthMode.byId("other").isEmpty());
    }
}
