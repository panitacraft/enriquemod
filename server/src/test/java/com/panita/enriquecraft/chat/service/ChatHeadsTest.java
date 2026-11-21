package com.panita.enriquecraft.chat.service;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHeadsTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private static GameProfile profile(String texturesSignature) {
        ImmutableMultimap.Builder<String, Property> properties = ImmutableMultimap.builder();
        properties.put("textures", new Property("textures", "VALUE", texturesSignature));
        properties.put("other", new Property("other", "x"));
        return new GameProfile(ID, "Alex", new PropertyMap(properties.build()));
    }

    @Test
    void theHeadIsAPlayerSpriteOfTheProfileFollowedByASpace() {
        Component head = ChatHeads.of(profile("SIGNATURE"));

        assertEquals(2, head.getSiblings().size());
        ObjectContents contents = assertInstanceOf(ObjectContents.class, head.getSiblings().get(0).getContents());
        PlayerSprite sprite = assertInstanceOf(PlayerSprite.class, contents.contents());
        assertTrue(sprite.hat());
        assertEquals(ID, sprite.player().partialProfile().id());
        assertEquals(" ", head.getSiblings().get(1).getString());
    }

    @Test
    void onlyTheSkinTravelAndWithoutItsSignature() {
        GameProfile slim = ChatHeads.slim(profile("SIGNATURE"));

        assertEquals(1, slim.properties().size());
        Property textures = slim.properties().get("textures").iterator().next();
        assertEquals("VALUE", textures.value());
        assertEquals(false, textures.hasSignature());
        assertEquals("Alex", slim.name());
        assertEquals(ID, slim.id());
    }

    @Test
    void aPlayerWithoutASkinStillHasAHead() {
        GameProfile slim = ChatHeads.slim(new GameProfile(ID, "Alex"));

        assertEquals(0, slim.properties().size());
        assertInstanceOf(ObjectContents.class, ChatHeads.of(new GameProfile(ID, "Alex")).getSiblings().get(0).getContents());
    }
}
