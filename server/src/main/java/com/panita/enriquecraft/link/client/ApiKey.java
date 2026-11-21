package com.panita.enriquecraft.link.client;

import com.panita.enriquecraft.core.framework.io.SecretFile;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Finds the service key of the server for the Panita API; see {@link SecretFile}.
 */
public final class ApiKey {

    public static final String ENVIRONMENT_VARIABLE = "ENRIQUECRAFT_LINK_API_KEY";

    private ApiKey() {
    }

    /** The key, or empty when there is none or what was found cannot be one. */
    public static Optional<String> find(Path file, UnaryOperator<String> environment) {
        return SecretFile.find(file, ENVIRONMENT_VARIABLE, environment);
    }
}
