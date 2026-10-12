package com.panita.enriquecraft.core.framework.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Finds a secret such as a service key or a bot token. A secret never lives in the config file: an environment
 * variable wins, then a one-line file next to the config. The file is only read, never created, and the secret is
 * never logged.
 */
public final class SecretFile {

    // Printable ASCII only: a secret travels in a header, and anything else would be a way to inject headers.
    private static final Pattern VALID = Pattern.compile("[\\x21-\\x7E]{1,200}");

    private SecretFile() {
    }

    /**
     * @param file        where the secret is kept
     * @param variable    the environment variable that overrides the file
     * @param environment how to read an environment variable
     * @return the secret, or empty when there is none or what was found cannot be one
     */
    public static Optional<String> find(Path file, String variable, UnaryOperator<String> environment) {
        String fromEnvironment = environment.apply(variable);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return valid(fromEnvironment);
        }
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return valid(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> valid(String raw) {
        String secret = raw.trim();
        return VALID.matcher(secret).matches() ? Optional.of(secret) : Optional.empty();
    }
}
