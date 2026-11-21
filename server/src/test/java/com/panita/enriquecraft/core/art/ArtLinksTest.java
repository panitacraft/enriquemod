package com.panita.enriquecraft.core.art;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtLinksTest {

    private static final String IMAGE = "https://res.cloudinary.com/panita/image/upload/v1/a.png";

    @Test
    void anAddressMayAskForASizeWithinTheLimits() {
        assertTrue(ArtLinks.isTrusted(IMAGE));
        assertTrue(ArtLinks.isTrusted(IMAGE + "#size=8"));
        assertTrue(ArtLinks.isTrusted(IMAGE + "#size=2"));
        assertTrue(ArtLinks.isTrusted(IMAGE + "#size=12"));
        assertFalse(ArtLinks.isTrusted(IMAGE + "#size=1"));
        assertFalse(ArtLinks.isTrusted(IMAGE + "#size=13"));
        assertFalse(ArtLinks.isTrusted(IMAGE + "#size=8x"));
        assertFalse(ArtLinks.isTrusted(IMAGE + "#anything"));
        assertFalse(ArtLinks.isTrusted(IMAGE + "#"));
    }

    @Test
    void theSizeIsReadFromTheEndOfTheAddress() {
        assertEquals(OptionalInt.of(8), ArtLinks.sizeOf(IMAGE + "#size=8"));
        assertEquals(OptionalInt.empty(), ArtLinks.sizeOf(IMAGE));
        assertEquals(OptionalInt.empty(), ArtLinks.sizeOf(IMAGE + "#size=99"));
    }

    @Test
    void whatIsFetchedLeavesTheSizeOut() {
        assertEquals(IMAGE, ArtLinks.withoutSize(IMAGE + "#size=8"));
        assertEquals(IMAGE, ArtLinks.withoutSize(IMAGE));
    }

    @Test
    void anAddressWithAnotherSizeIsAnotherArt() {
        assertFalse(ArtLinks.idOf(IMAGE + "#size=8").equals(ArtLinks.idOf(IMAGE)));
    }

    @Test
    void aHostThatIsNotTrustedStaysUntrustedWhateverItsSize() {
        assertFalse(ArtLinks.isTrusted("https://example.com/a.png#size=8"));
        assertFalse(ArtLinks.isTrusted("http://res.cloudinary.com/a.png#size=8"));
    }
}
