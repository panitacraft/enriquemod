package com.panita.enriquecraft.core.framework.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigBackupsTest {

    @TempDir
    Path directory;

    private List<String> copies() throws IOException {
        try (var files = Files.list(directory.resolve(ConfigBackups.DIRECTORY))) {
            return files.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void aCopyIsNamedAfterTheFileAndTheMoment() throws IOException {
        Path copy = new ConfigBackups(directory.resolve("enriquecraft.json5")).save("old text", LocalDateTime.of(2026, 11, 23, 18, 30, 5));

        assertEquals("enriquecraft-20261123-183005.json5", copy.getFileName().toString());
        assertEquals("old text", Files.readString(copy, StandardCharsets.UTF_8));
    }

    @Test
    void twoCopiesInTheSameSecondDoNotReplaceEachOther() throws IOException {
        ConfigBackups backups = new ConfigBackups(directory.resolve("enriquecraft.json5"));
        LocalDateTime moment = LocalDateTime.of(2026, 11, 23, 18, 30, 5);

        backups.save("first", moment);
        backups.save("second", moment);

        assertEquals(List.of("enriquecraft-20261123-183005-2.json5", "enriquecraft-20261123-183005.json5"), copies());
    }

    @Test
    void onlyTheNewestCopiesAreKept() throws IOException {
        ConfigBackups backups = new ConfigBackups(directory.resolve("enriquecraft.json5"));
        for (int day = 1; day <= ConfigBackups.KEPT + 3; day++) {
            backups.save("copy " + day, LocalDateTime.of(2026, 11, day, 12, 0, 0));
        }

        List<String> kept = copies();
        assertEquals(ConfigBackups.KEPT, kept.size());
        assertEquals("enriquecraft-20261104-120000.json5", kept.get(0));
        assertEquals("enriquecraft-20261113-120000.json5", kept.get(kept.size() - 1));
    }

    @Test
    void theCopiesOfAnotherFileAreNeverRemoved() throws IOException {
        ConfigBackups messages = new ConfigBackups(directory.resolve("enriquecraft-messages.json5"));
        messages.save("messages", LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        ConfigBackups main = new ConfigBackups(directory.resolve("enriquecraft.json5"));
        for (int day = 1; day <= ConfigBackups.KEPT + 2; day++) {
            main.save("copy " + day, LocalDateTime.of(2026, 11, day, 12, 0, 0));
        }

        assertTrue(copies().contains("enriquecraft-messages-20260101-000000.json5"));
    }

    @Test
    void aNewFileIsNeverCopied() throws IOException {
        new ConfigManager(directory).bind("core", ConfigManagerTest.SampleConfig.class);

        assertFalse(Files.exists(directory.resolve(ConfigBackups.DIRECTORY)));
    }

    @Test
    void addingSettingsToAnOldFileKeepsACopyOfItAsItWas() throws IOException {
        String old = "{ \"core\": { \"messages\": { \"prefix\": \"Hello\" }, \"limits\": { \"max\": 25 } } }\n";
        Files.writeString(directory.resolve(ConfigManager.FILE_NAME), old, StandardCharsets.UTF_8);

        new ConfigManager(directory).bind("core", ConfigManagerTest.SampleConfig.class);

        List<String> copies = copies();
        assertEquals(1, copies.size());
        assertEquals(old, Files.readString(directory.resolve(ConfigBackups.DIRECTORY).resolve(copies.get(0)), StandardCharsets.UTF_8));
    }

    @Test
    void sectionsBoundOneAfterAnotherMakeOneCopyOfTheOriginalFile() throws IOException {
        String old = "{ \"core\": { \"messages\": { \"prefix\": \"Hello\" } } }\n";
        Files.writeString(directory.resolve(ConfigManager.FILE_NAME), old, StandardCharsets.UTF_8);
        ConfigManager manager = new ConfigManager(directory);

        manager.bind("core", ConfigManagerTest.SampleConfig.class);
        manager.bind("other", ConfigManagerTest.OtherConfig.class);

        assertEquals(1, copies().size());
    }

    @Test
    void anEditMadeAfterTheModWroteTheFileIsCopiedBeforeTheNextRewrite() throws IOException {
        Path file = directory.resolve(ConfigManager.FILE_NAME);
        ConfigManager manager = new ConfigManager(directory);
        manager.bind("core", ConfigManagerTest.SampleConfig.class);
        Files.writeString(file, "{ \"core\": { \"messages\": { \"prefix\": \"Edited\" } } }\n", StandardCharsets.UTF_8);

        manager.reload();

        assertEquals(1, copies().size());
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"prefix\": \"Edited\""));
    }
}
