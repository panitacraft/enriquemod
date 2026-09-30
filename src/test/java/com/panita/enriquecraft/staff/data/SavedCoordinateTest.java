package com.panita.enriquecraft.staff.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavedCoordinateTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    private static SavedCoordinate sample() {
        return new SavedCoordinate("Base", Level.END, 1.5, 70.0, -3.25, 45.0F, -10.0F,
                UUID.fromString("11111111-2222-3333-4444-555555555555"), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), Items.BEACON);
    }

    @Test
    void roundTripKeepsEveryField() {
        JsonElement json = SavedCoordinate.CODEC.encodeStart(JsonOps.INSTANCE, sample()).getOrThrow();

        assertEquals(sample(), SavedCoordinate.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void fileFormatIsReadable() {
        JsonObject json = SavedCoordinate.CODEC.encodeStart(JsonOps.INSTANCE, sample()).getOrThrow().getAsJsonObject();

        assertEquals("minecraft:the_end", json.get("dimension").getAsString());
        assertEquals("minecraft:beacon", json.get("icon").getAsString());
        assertEquals("2026-09-30T04:12:00Z", json.get("savedAt").getAsString());
        assertEquals("11111111-2222-3333-4444-555555555555", json.get("savedBy").getAsString());
        assertEquals(1.5, json.get("x").getAsDouble());
    }

    @Test
    void aMissingFieldIsAnErrorNotAPartialRecord() {
        JsonObject json = SavedCoordinate.CODEC.encodeStart(JsonOps.INSTANCE, sample()).getOrThrow().getAsJsonObject();
        json.remove("icon");

        assertTrue(SavedCoordinate.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void anUnknownIconIsAnError() {
        JsonObject json = SavedCoordinate.CODEC.encodeStart(JsonOps.INSTANCE, sample()).getOrThrow().getAsJsonObject();
        json.addProperty("icon", "minecraft:not_an_item");

        assertTrue(SavedCoordinate.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }
}
