package com.panita.enriquecraft.link.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApiKeyTest {

    @TempDir
    Path directory;

    private Optional<String> find(Map<String, String> environment) {
        return ApiKey.find(directory.resolve("key"), environment::get);
    }

    @Test
    void readsTheFileAndTrimsIt() throws IOException {
        Files.writeString(directory.resolve("key"), "  abc123\n");

        assertEquals(Optional.of("abc123"), find(Map.of()));
    }

    @Test
    void theEnvironmentWinsOverTheFile() throws IOException {
        Files.writeString(directory.resolve("key"), "from-file");

        assertEquals(Optional.of("from-env"), find(Map.of(ApiKey.ENVIRONMENT_VARIABLE, "from-env")));
    }

    @Test
    void noFileAndNoVariableIsNoKeyAndNothingIsCreated() {
        assertEquals(Optional.empty(), find(Map.of()));
        assertFalse(Files.exists(directory.resolve("key")));
    }

    @Test
    void aBlankFileOrVariableIsNoKey() throws IOException {
        Files.writeString(directory.resolve("key"), "  \n");

        assertEquals(Optional.empty(), find(Map.of()));
        assertEquals(Optional.empty(), find(Map.of(ApiKey.ENVIRONMENT_VARIABLE, "   ")));
    }

    @Test
    void aValueThatCouldInjectAHeaderIsNoKey() throws IOException {
        Files.writeString(directory.resolve("key"), "abc\r\nX-Evil: 1");

        assertEquals(Optional.empty(), find(Map.of()));
        assertEquals(Optional.empty(), find(Map.of(ApiKey.ENVIRONMENT_VARIABLE, "two words")));
    }
}
