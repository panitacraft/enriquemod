package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads which skin a player wears from the {@code textures} property of their profile. SkinsRestorer puts the skin a player
 * chose there, when they join and each time they change it, so the game's profile is always what the server shows them with.
 * Only the hash of the skin is taken, never the address, so the bot decides which service draws the head.
 */
final class SkinTextures {

    private static final String PROPERTY = "textures";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final String TEXTURES_HOST = "textures.minecraft.net";
    private static final String TEXTURE_PATH = "/texture/";

    private SkinTextures() {
    }

    /** The hash of the skin in a player's profile as it is now, if it has one. */
    static Optional<String> of(GameProfile profile) {
        return profile.properties().get(PROPERTY).stream()
                .map(property -> hashOf(property.value()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** The hash of the skin, if the value is a textures document of Mojang's with one. Anything else gives nothing. */
    static Optional<String> hashOf(String propertyValue) {
        try {
            String json = new String(Base64.getDecoder().decode(propertyValue), StandardCharsets.UTF_8);
            JsonObject skin = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN");
            JsonElement address = skin.get("url");
            return address == null ? Optional.empty() : hashOfAddress(address.getAsString());
        } catch (RuntimeException e) {
            // Not base64, not JSON or not the shape of a textures document: the player simply has no skin to report
            return Optional.empty();
        }
    }

    private static Optional<String> hashOfAddress(String address) {
        URI uri = URI.create(address);
        boolean mojang = ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && TEXTURES_HOST.equals(uri.getHost());
        String path = uri.getPath();
        if (!mojang || path == null || !path.startsWith(TEXTURE_PATH)) {
            return Optional.empty();
        }
        String hash = path.substring(TEXTURE_PATH.length());
        return HASH.matcher(hash).matches() ? Optional.of(hash) : Optional.empty();
    }
}
