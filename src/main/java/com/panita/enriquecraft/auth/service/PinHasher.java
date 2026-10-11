package com.panita.enriquecraft.auth.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Turns PINs and addresses into values that are safe to store. A PIN has only a few thousand
 * possibilities, so whoever gets hold of a data file could try them all; two things make that slower
 * and pointless on its own: every PIN has its own random salt and a slow key derivation, and a secret
 * "pepper" kept in a file outside the world data is mixed into everything, so the world data alone
 * proves nothing.
 */
public final class PinHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 60_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int PEPPER_BYTES = 32;

    private final byte[] pepper;
    private final SecureRandom random = new SecureRandom();

    public PinHasher(byte[] pepper) {
        this.pepper = pepper.clone();
    }

    /**
     * Reads the pepper from the file, creating it with random bytes the first time. A file that exists
     * but cannot be used stops the start: silently making a new pepper would invalidate every PIN.
     *
     * @throws IllegalStateException if the file cannot be read, written or understood
     */
    public static PinHasher withPepperFile(Path file) {
        try {
            if (Files.exists(file)) {
                byte[] pepper = Base64.getDecoder().decode(Files.readString(file, StandardCharsets.UTF_8).trim());
                if (pepper.length < PEPPER_BYTES) {
                    throw new IllegalStateException("The pepper file " + file + " is too short");
                }
                return new PinHasher(pepper);
            }
            byte[] pepper = new byte[PEPPER_BYTES];
            new SecureRandom().nextBytes(pepper);
            Files.createDirectories(file.getParent());
            Files.writeString(file, Base64.getEncoder().encodeToString(pepper) + "\n", StandardCharsets.UTF_8);
            return new PinHasher(pepper);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("The pepper file " + file + " cannot be used: " + e.getMessage(), e);
        }
    }

    /** A new random salt, in Base64. */
    public String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hash(String pin, String salt) {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] material = Arrays.copyOf(saltBytes, saltBytes.length + pepper.length);
        System.arraycopy(pepper, 0, material, saltBytes.length, pepper.length);
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), material, ITERATIONS, KEY_BITS);
        try {
            return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("The JDK cannot derive keys with " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Compares in constant time, so the time taken says nothing about how much of a PIN was right. */
    public boolean matches(String pin, String salt, String expectedHash) {
        return MessageDigest.isEqual(hash(pin, salt).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    /** A keyed hash of an address, so sessions can compare addresses without the data holding them. */
    public String fingerprint(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pepper);
            digest.update(address.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("The JDK has no SHA-256", e);
        }
    }
}
