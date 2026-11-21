package com.panita.enriquecraft.link.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiKeyTest {

    @TempDir
    Path directory;

    @Test
    void theKeyComesFromItsOwnEnvironmentVariableOrFile() throws IOException {
        Path file = directory.resolve("key");
        Files.writeString(file, "from-file\n");

        assertEquals(Optional.of("from-file"), ApiKey.find(file, Map.<String, String>of()::get));
        assertEquals(Optional.of("from-env"), ApiKey.find(file, Map.of("ENRIQUECRAFT_LINK_API_KEY", "from-env")::get));
    }
}
