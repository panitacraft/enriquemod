package com.panita.enriquecraft.link.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Finds the service key of the server. It is a secret, so it never lives in the config file: an environment
 * variable wins, then a one-line file next to the config. The file is only read, never created.
 */
public final class ApiKey {

    public static final String ENVIRONMENT_VARIABLE = "ENRIQUECRAFT_LINK_API_KEY";

    // Printable ASCII only: the key travels in a header, and anything else would be a way to inject headers.
    private static final Pattern VALID = Pattern.compile("[\\x21-\\x7E]{1,200}");

    private ApiKey() {
    }

    /** The key, or empty when there is none or what was found cannot be one. */
    public static Optional<String> find(Path file, UnaryOperator<String> environment) {
        String fromEnvironment = environment.apply(ENVIRONMENT_VARIABLE);
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
        String key = raw.trim();
        return VALID.matcher(key).matches() ? Optional.of(key) : Optional.empty();
    }
}
