package com.panita.enriquecraft.chat.service;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.chat.data.KnownSkins;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinMemoryTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private final AtomicReference<KnownSkins> stored = new AtomicReference<>(KnownSkins.EMPTY);
    private final AtomicInteger writes = new AtomicInteger();
    private final SkinMemory memory = new SkinMemory(stored::get, skins -> {
        writes.incrementAndGet();
        stored.set(skins);
    });

    private static GameProfile profile(String name, String textures) {
        ImmutableMultimap.Builder<String, Property> properties = ImmutableMultimap.builder();
        if (textures != null) {
            properties.put("textures", new Property("textures", textures, "SIGNATURE"));
        }
        return new GameProfile(ID, name, new PropertyMap(properties.build()));
    }

    @Test
    void theLastSkinOfAPlayerIsFoundByNameWhateverTheCapitalization() {
        memory.remember(profile("Alex", "VALUE"));

        GameProfile found = memory.profileOf("aLEX").orElseThrow();

        assertEquals("Alex", found.name());
        assertEquals(ID, found.id());
        assertEquals("VALUE", found.properties().get("textures").iterator().next().value());
        assertEquals(false, found.properties().get("textures").iterator().next().hasSignature());
    }

    @Test
    void aPlayerWithoutASkinIsNotRemembered() {
        memory.remember(profile("Alex", null));

        assertEquals(Optional.empty(), memory.profileOf("Alex"));
        assertEquals(0, writes.get());
    }

    @Test
    void aSkinThatDidNotChangeIsNotWrittenAgainAndANewOneReplacesIt() {
        memory.remember(profile("Alex", "ONE"));
        memory.remember(profile("Alex", "ONE"));
        memory.remember(profile("Alex", "TWO"));

        assertEquals(2, writes.get());
        assertEquals(1, stored.get().entries().size());
        assertEquals("TWO", memory.profileOf("Alex").orElseThrow().properties().get("textures").iterator().next().value());
    }

    @Test
    void aPlayerNeverSeenHasNone() {
        assertEquals(Optional.empty(), memory.profileOf("Nobody"));
    }

    @Test
    void whatIsKeptIsReadBackExactly() {
        memory.remember(profile("Alex", "ONE"));
        memory.remember(profile("Sam", "TWO"));

        JsonElement written = KnownSkins.CODEC.encodeStart(JsonOps.INSTANCE, stored.get()).getOrThrow();

        assertEquals(stored.get(), KnownSkins.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }
}
