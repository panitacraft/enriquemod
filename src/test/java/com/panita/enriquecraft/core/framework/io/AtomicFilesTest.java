package com.panita.enriquecraft.core.framework.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicFilesTest {

    @TempDir
    Path directory;

    @Test
    void writesTextAndCreatesMissingDirectories() throws IOException {
        Path file = directory.resolve("a/b/data.json");

        AtomicFiles.write(file, "{ \"key\": \"value\" }");

        assertEquals("{ \"key\": \"value\" }", Files.readString(file));
    }

    @Test
    void replacesExistingContentInFull() throws IOException {
        Path file = directory.resolve("data.json");
        AtomicFiles.write(file, "a much longer first version of the content");

        AtomicFiles.write(file, "short");

        assertEquals("short", Files.readString(file));
    }

    @Test
    void keepsNonAsciiTextIntact() throws IOException {
        Path file = directory.resolve("data.json");

        AtomicFiles.write(file, "Configuraci\u00f3n \u2714");

        assertEquals("Configuraci\u00f3n \u2714", Files.readString(file));
    }

    @Test
    void leavesNoTemporaryFileBehind() throws IOException {
        Path file = directory.resolve("data.json");

        AtomicFiles.write(file, "content");

        try (var files = Files.list(directory)) {
            assertEquals(List.of("data.json"), files.map(path -> path.getFileName().toString()).toList());
        }
    }
}
