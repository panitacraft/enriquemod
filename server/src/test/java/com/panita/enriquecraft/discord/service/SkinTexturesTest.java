package com.panita.enriquecraft.discord.service;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinTexturesTest {

    private static final String HASH = "292009a4925b58f02c77dadc3ecef07ea4c7472f64e0fdc32ce5522489362680";

    private static String property(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String skinAt(String url) {
        return property("{\"timestamp\":1,\"profileId\":\"x\",\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}");
    }

    @Test
    void takesTheHashOfTheSkinFromAMojangTexturesProperty() {
        assertEquals(Optional.of(HASH), SkinTextures.hashOf(skinAt("http://textures.minecraft.net/texture/" + HASH)));
        assertEquals(Optional.of(HASH), SkinTextures.hashOf(skinAt("https://textures.minecraft.net/texture/" + HASH)));
    }

    @Test
    void ignoresTheCapeAndTheSlimModel() {
        String value = property("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + HASH
                + "\",\"metadata\":{\"model\":\"slim\"}},\"CAPE\":{\"url\":\"http://textures.minecraft.net/texture/" + "a".repeat(64) + "\"}}}");

        assertEquals(Optional.of(HASH), SkinTextures.hashOf(value));
    }

    @Test
    void hasNoSkinWhenThePlayerHasOnlyACape() {
        assertTrue(SkinTextures.hashOf(property("{\"textures\":{\"CAPE\":{\"url\":\"http://textures.minecraft.net/texture/" + HASH + "\"}}}")).isEmpty());
    }

    @Test
    void refusesWhatIsNotAMojangTexture() {
        assertTrue(SkinTextures.hashOf(skinAt("http://evil.example/texture/" + HASH)).isEmpty());
        assertTrue(SkinTextures.hashOf(skinAt("http://textures.minecraft.net.evil.example/texture/" + HASH)).isEmpty());
        assertTrue(SkinTextures.hashOf(skinAt("http://textures.minecraft.net/texture/not-a-hash")).isEmpty());
        assertTrue(SkinTextures.hashOf(skinAt("http://textures.minecraft.net/texture/" + HASH + "/../x")).isEmpty());
        assertTrue(SkinTextures.hashOf(skinAt("http://textures.minecraft.net/texture/" + HASH.toUpperCase())).isEmpty());
    }

    @Test
    void readsTheSkinOfAProfileAndNothingFromOneWithoutTextures() {
        UUID id = UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498");
        Property textures = new Property("textures", skinAt("http://textures.minecraft.net/texture/" + HASH));
        Property other = new Property("other", skinAt("http://textures.minecraft.net/texture/" + "b".repeat(64)));

        assertEquals(Optional.of(HASH), SkinTextures.of(new GameProfile(id, "Javithor360", new PropertyMap(ImmutableMultimap.of("other", other, "textures", textures)))));
        assertTrue(SkinTextures.of(new GameProfile(id, "Javithor360", new PropertyMap(ImmutableMultimap.of("other", other)))).isEmpty());
        assertTrue(SkinTextures.of(new GameProfile(id, "Javithor360")).isEmpty());
    }

    @Test
    void refusesAValueThatIsNotATexturesDocument() {
        assertTrue(SkinTextures.hashOf("not base64 !!").isEmpty());
        assertTrue(SkinTextures.hashOf(property("not json")).isEmpty());
        assertTrue(SkinTextures.hashOf(property("[]")).isEmpty());
        assertTrue(SkinTextures.hashOf(property("{}")).isEmpty());
        assertTrue(SkinTextures.hashOf(property("{\"textures\":{\"SKIN\":{}}}")).isEmpty());
        assertTrue(SkinTextures.hashOf(property("{\"textures\":{\"SKIN\":{\"url\":5}}}")).isEmpty());
    }
}
