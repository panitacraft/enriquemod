package com.panita.enriquecraft.profile.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedProfilesTest {

    @Test
    void whatWasCachedIsReadBackExactly() {
        ApiEmblem staff = new ApiEmblem("staff", "Staff", Optional.of("Runs the server"), Optional.of("https://example.com/a.png"),
                EmblemCategory.LOCAL_EMBLEM, 3, Optional.of("enriquecraft6"));
        ApiEmblem title = new ApiEmblem("title", "Title", Optional.empty(), Optional.empty(), EmblemCategory.TITLE, 9, Optional.empty());
        CachedProfiles cached = new CachedProfiles(Map.of(
                UUID.randomUUID(), new CachedProfiles.Entry(Optional.of(new ModProfile(7, Optional.of("Alex"), "alex", List.of(staff, title))),
                        Instant.parse("2026-11-09T12:00:00Z")),
                UUID.randomUUID(), new CachedProfiles.Entry(Optional.empty(), Instant.parse("2026-11-09T13:00:00Z"))));

        JsonElement written = CachedProfiles.CODEC.encodeStart(JsonOps.INSTANCE, cached).getOrThrow();

        assertEquals(cached, CachedProfiles.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void anUnknownEmblemCategoryIsRefused() {
        JsonElement written = CachedProfiles.CODEC.encodeStart(JsonOps.INSTANCE, new CachedProfiles(Map.of(UUID.randomUUID(),
                new CachedProfiles.Entry(Optional.of(new ModProfile(1, Optional.empty(), "a", List.of(
                        new ApiEmblem("x", "X", Optional.empty(), Optional.empty(), EmblemCategory.EMBLEM, 0, Optional.empty())))),
                        Instant.EPOCH)))).getOrThrow();
        String broken = written.toString().replace("\"emblem\"", "\"nonsense\"");

        assertTrue(CachedProfiles.CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(broken)).error().isPresent());
    }
}
