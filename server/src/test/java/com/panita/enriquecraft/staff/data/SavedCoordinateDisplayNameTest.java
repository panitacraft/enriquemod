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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The name staff see, kept apart from the id commands use. */
class SavedCoordinateDisplayNameTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    private static SavedCoordinate sample() {
        return new SavedCoordinate("base", Level.END, 1.5, 70.0, -3.25, 45.0F, -10.0F,
                UUID.fromString("11111111-2222-3333-4444-555555555555"), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), Items.BEACON);
    }

    private static CompoundTag encode(SavedCoordinate coordinate) {
        return (CompoundTag) SavedCoordinate.CODEC.encodeStart(NbtOps.INSTANCE, coordinate).getOrThrow();
    }

    @Test
    void aNewCoordinateIsShownUnderItsId() {
        assertEquals("base", sample().displayName());
    }

    @Test
    void aDisplayNameEqualToTheIdIsNotWrittenToTheFile() {
        assertFalse(encode(sample()).contains("displayName"));
    }

    @Test
    void aFileFromBeforeDisplayNamesExistedStillLoadsUnderTheId() {
        Tag tag = encode(sample());

        assertEquals("base", SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().displayName());
    }

    @Test
    void aCustomDisplayNameIsWrittenAndReadBack() {
        SavedCoordinate renamed = sample().withDisplayName("Mi base secreta");

        CompoundTag tag = encode(renamed);

        assertEquals("Mi base secreta", tag.getStringOr("displayName", ""));
        assertEquals(renamed, SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test
    void renamingNeverChangesTheId() {
        assertEquals("base", sample().withDisplayName("Otra cosa").name());
    }

    @Test
    void changingTheIconKeepsTheDisplayName() {
        assertEquals("Mi base", sample().withDisplayName("Mi base").withIcon(Items.DIAMOND).displayName());
    }

    @Test
    void displayNamesAreCheckedForLengthAndContent() {
        assertTrue(SavedCoordinate.isValidDisplayName("Mi base"));
        assertTrue(SavedCoordinate.isValidDisplayName("x".repeat(SavedCoordinate.MAX_DISPLAY_NAME)));
        assertFalse(SavedCoordinate.isValidDisplayName(""));
        assertFalse(SavedCoordinate.isValidDisplayName("   "));
        assertFalse(SavedCoordinate.isValidDisplayName("x".repeat(SavedCoordinate.MAX_DISPLAY_NAME + 1)));
        assertFalse(SavedCoordinate.isValidDisplayName("a\nb"));
    }

    @Test
    void aHeadIconWritesThePlayerAndAFileFromBeforeHeadsStillLoads() {
        SavedCoordinate head = sample().withHeadOf(new PlayerRef("Notch", UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")));

        Tag tag = SavedCoordinate.CODEC.encodeStart(NbtOps.INSTANCE, head).getOrThrow();
        Tag old = SavedCoordinate.CODEC.encodeStart(NbtOps.INSTANCE, sample()).getOrThrow();

        assertEquals("Notch", ((CompoundTag) tag).getCompoundOrEmpty("iconPlayer").getStringOr("name", ""));
        assertFalse(((CompoundTag) old).contains("iconPlayer"), "nothing is written when there is no head");
        assertEquals(head, SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
        assertTrue(SavedCoordinate.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow().iconPlayer().isEmpty());
    }

    @Test
    void playerNamesAreThreeToSixteenLettersDigitsOrUnderscores() {
        assertTrue(SavedCoordinate.isValidPlayerName("Notch"));
        assertTrue(SavedCoordinate.isValidPlayerName("a_b"));
        assertTrue(SavedCoordinate.isValidPlayerName("x".repeat(16)));
        assertFalse(SavedCoordinate.isValidPlayerName("ab"));
        assertFalse(SavedCoordinate.isValidPlayerName("x".repeat(17)));
        assertFalse(SavedCoordinate.isValidPlayerName("has space"));
        assertFalse(SavedCoordinate.isValidPlayerName("ñandú"));
    }
}
