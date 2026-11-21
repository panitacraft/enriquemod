package com.panita.enriquecraft.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinHasherTest {

    @TempDir
    Path directory;

    private static PinHasher hasher(int seed) {
        byte[] pepper = new byte[32];
        Arrays.fill(pepper, (byte) seed);
        return new PinHasher(pepper);
    }

    @Test
    void theRightPinMatchesAndAWrongOneDoesNot() {
        PinHasher hasher = hasher(1);
        String salt = hasher.newSalt();
        String hash = hasher.hash("1234", salt);

        assertTrue(hasher.matches("1234", salt, hash));
        assertFalse(hasher.matches("1235", salt, hash));
    }

    @Test
    void theSameSaltAndPinGiveTheSameHashButAnotherSaltDoesNot() {
        PinHasher hasher = hasher(1);
        String salt = hasher.newSalt();

        assertEquals(hasher.hash("1234", salt), hasher.hash("1234", salt));
        assertNotEquals(hasher.hash("1234", salt), hasher.hash("1234", hasher.newSalt()));
    }

    @Test
    void anotherPepperGivesAnotherHash() {
        String salt = hasher(1).newSalt();

        assertNotEquals(hasher(1).hash("1234", salt), hasher(2).hash("1234", salt));
    }

    @Test
    void thePinIsNotInTheHash() {
        PinHasher hasher = hasher(1);

        assertFalse(hasher.hash("1234", hasher.newSalt()).contains("1234"));
    }

    @Test
    void fingerprintsAreStableAndDependOnTheAddressAndThePepper() {
        assertEquals(hasher(1).fingerprint("10.0.0.1"), hasher(1).fingerprint("10.0.0.1"));
        assertNotEquals(hasher(1).fingerprint("10.0.0.1"), hasher(1).fingerprint("10.0.0.2"));
        assertNotEquals(hasher(1).fingerprint("10.0.0.1"), hasher(2).fingerprint("10.0.0.1"));
    }

    @Test
    void thePepperFileIsCreatedOnceAndReused() {
        Path file = directory.resolve("config").resolve("auth.key");
        PinHasher first = PinHasher.withPepperFile(file);
        String salt = first.newSalt();
        String hash = first.hash("4321", salt);

        PinHasher second = PinHasher.withPepperFile(file);

        assertTrue(Files.exists(file));
        assertTrue(second.matches("4321", salt, hash));
    }

    @Test
    void aPepperFileThatCannotBeUsedStopsTheStartInsteadOfBeingReplaced() throws IOException {
        Path file = directory.resolve("auth.key");
        Files.writeString(file, "not base64 !!");

        assertThrows(IllegalStateException.class, () -> PinHasher.withPepperFile(file));
        assertEquals("not base64 !!", Files.readString(file));
    }
}
