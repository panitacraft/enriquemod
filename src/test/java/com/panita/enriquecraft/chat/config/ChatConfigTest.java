package com.panita.enriquecraft.chat.config;

import com.panita.enriquecraft.core.framework.config.ConfigIssue;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatConfigTest {

    @TempDir
    Path directory;

    @Test
    void defaultsCarryTheStandardEmojis() {
        ConfigManager manager = new ConfigManager(directory);

        ChatConfig config = manager.bind("chat", ChatConfig.class);

        assertEquals("<red>❤", config.emojis.get().get(":heart:"));
        assertEquals("<red>❤", config.emojis.get().get("<3"));
        assertTrue(manager.reload().isClean());
    }

    @Test
    void emojisWrittenByTheAdministratorReplaceTheDefaults() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"),
                "{ \"chat\": { \"emojis\": { \":ok:\": \"<green>✔\" } } }");

        ChatConfig config = new ConfigManager(directory).bind("chat", ChatConfig.class);

        assertEquals(Map.of(":ok:", "<green>✔"), config.emojis.get());
    }

    @Test
    void triggerWithSpacesIsRejectedAndTheDefaultsStay() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"),
                "{ \"chat\": { \"emojis\": { \"a b\": \"x\" } } }");
        ConfigManager manager = new ConfigManager(directory);

        ChatConfig config = manager.bind("chat", ChatConfig.class);
        ConfigReport report = manager.reload();

        assertEquals("<red>❤", config.emojis.get().get(":heart:"));
        assertEquals(ConfigIssue.Kind.INVALID_VALUE, report.issues().getFirst().kind());
        assertEquals("chat.emojis", report.issues().getFirst().path());
    }
}
