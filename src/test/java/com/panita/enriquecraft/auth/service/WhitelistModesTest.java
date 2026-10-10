package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistModesTest {

    private static final NameAndId PREMIUM = new NameAndId(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360");
    private static final NameAndId CRACKED = NameAndId.createOffline("Tezzlar");

    @Test
    void anEntryGivesTheModeOfItsUuid() {
        assertEquals(Optional.of(AuthMode.PREMIUM), WhitelistModes.fromEntries(List.of(PREMIUM, CRACKED), "Javithor360"));
        assertEquals(Optional.of(AuthMode.CRACKED), WhitelistModes.fromEntries(List.of(PREMIUM, CRACKED), "Tezzlar"));
    }

    @Test
    void namesMatchIgnoringCase() {
        assertEquals(Optional.of(AuthMode.CRACKED), WhitelistModes.fromEntries(List.of(CRACKED), "tEZZLAR"));
    }

    @Test
    void anUnlistedNameHasNoMode() {
        assertTrue(WhitelistModes.fromEntries(List.of(PREMIUM), "Nobody").isEmpty());
        assertTrue(WhitelistModes.fromEntries(List.of(), "Nobody").isEmpty());
    }

    @Test
    void whenEntriesDisagreePremiumWins() {
        NameAndId crackedJavi = NameAndId.createOffline("Javithor360");

        assertEquals(Optional.of(AuthMode.PREMIUM), WhitelistModes.fromEntries(List.of(crackedJavi, PREMIUM), "Javithor360"));
        assertEquals(Optional.of(AuthMode.PREMIUM), WhitelistModes.fromEntries(List.of(PREMIUM, crackedJavi), "Javithor360"));
    }
}
