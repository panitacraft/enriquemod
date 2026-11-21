package com.panita.enriquecraft.profile.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileDetailsBookTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private static ProfileDetails sample() {
        return new ProfileDetails(Optional.of(new ProfileDetails.Base(Level.NETHER, 159, 62, -231)), "Hola\nque tal", "ella", false);
    }

    @Test
    void aPlayerWhoNeverChangedAnythingHasTheDefaults() {
        assertEquals(ProfileDetails.DEFAULT, ProfileDetailsBook.EMPTY.of(7));
        assertTrue(ProfileDetails.DEFAULT.shareLocation());
    }

    @Test
    void detailsAreReadBackExactly() {
        ProfileDetailsBook book = ProfileDetailsBook.EMPTY.with(7, sample()).with(8, ProfileDetails.DEFAULT.withBio("otro"));

        JsonElement written = ProfileDetailsBook.CODEC.encodeStart(JsonOps.INSTANCE, book).getOrThrow();

        assertEquals(book, ProfileDetailsBook.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
        assertEquals(sample(), book.of(7));
    }

    @Test
    void onlyWhatDiffersFromTheDefaultIsWritten() {
        ProfileDetailsBook book = ProfileDetailsBook.EMPTY.with(7, ProfileDetails.DEFAULT.withBio("solo texto"));

        String written = ProfileDetailsBook.CODEC.encodeStart(JsonOps.INSTANCE, book).getOrThrow().toString();

        assertTrue(written.contains("solo texto"));
        assertFalse(written.contains("base"));
        assertFalse(written.contains("shareLocation"));
    }

    @Test
    void detailsBackAtTheDefaultAreNotKept() {
        ProfileDetailsBook book = ProfileDetailsBook.EMPTY.with(7, sample()).with(7, ProfileDetails.DEFAULT);

        assertEquals(ProfileDetailsBook.EMPTY, book);
    }

    @Test
    void changingOnePlayerLeavesTheOthersAlone() {
        ProfileDetailsBook book = ProfileDetailsBook.EMPTY.with(7, sample()).with(8, ProfileDetails.DEFAULT.withBio("otro"));

        ProfileDetailsBook changed = book.with(7, sample().withShareLocation(true));

        assertEquals("otro", changed.of(8).bio());
        assertTrue(changed.of(7).shareLocation());
    }
}
