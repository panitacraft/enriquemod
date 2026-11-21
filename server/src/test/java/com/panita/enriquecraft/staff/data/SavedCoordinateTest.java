package com.panita.enriquecraft.staff.data;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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

    private static CompoundTag encoded() {
        return (CompoundTag) SavedCoordinate.CODEC.encodeStart(NbtOps.INSTANCE, sample()).getOrThrow();
    }

    @Test
    void roundTripKeepsEveryField() {
        Tag tag = SavedCoordinate.CODEC.encodeStart(NbtOps.INSTANCE, sample()).getOrThrow();

        assertEquals(sample(), SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test
    void fileFormatIsReadable() {
        CompoundTag tag = encoded();

        assertEquals("minecraft:the_end", tag.getStringOr("dimension", ""));
        assertEquals("minecraft:beacon", tag.getStringOr("icon", ""));
        assertEquals("2026-09-30T04:12:00Z", tag.getStringOr("savedAt", ""));
        assertEquals("11111111-2222-3333-4444-555555555555", tag.getStringOr("savedBy", ""));
        assertEquals(1.5, tag.getDoubleOr("x", 0));
    }

    @Test
    void aMissingFieldIsAnErrorNotAPartialRecord() {
        CompoundTag tag = encoded();
        tag.remove("icon");

        assertTrue(SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
    }

    @Test
    void anUnknownIconIsAnError() {
        CompoundTag tag = encoded();
        tag.putString("icon", "minecraft:not_an_item");

        assertTrue(SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
    }
}
