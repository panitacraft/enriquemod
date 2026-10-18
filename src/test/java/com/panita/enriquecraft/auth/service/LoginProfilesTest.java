package com.panita.enriquecraft.auth.service;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LoginProfilesTest {

    private static final UUID ID = UUID.fromString("8c0f4782-ad24-47f5-b874-885e93c5120c");
    private static final GameProfile AUTHENTICATED = new GameProfile(ID, "Javithor360",
            new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", "own-skin"))));
    private static final GameProfile WITH_ANOTHER_SKIN = new GameProfile(ID, "Javithor360",
            new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", "chosen-skin"))));

    @Test
    void theSkinAModGaveTheLoginReplacesTheOneOfTheAccount() {
        assertSame(WITH_ANOTHER_SKIN, LoginProfiles.carry(AUTHENTICATED, WITH_ANOTHER_SKIN));
    }

    @Test
    void aProfileOfAnotherPlayerOrNameNeverReplacesTheOneThatWasAuthenticated() {
        GameProfile otherId = new GameProfile(UUID.randomUUID(), "Javithor360");
        GameProfile otherName = new GameProfile(ID, "Tezzlar");

        assertSame(AUTHENTICATED, LoginProfiles.carry(AUTHENTICATED, otherId));
        assertSame(AUTHENTICATED, LoginProfiles.carry(AUTHENTICATED, otherName));
    }

    @Test
    void nothingIsMadeUpWhenNoProfileWasAuthenticated() {
        assertNull(LoginProfiles.carry(null, WITH_ANOTHER_SKIN));
    }
}
