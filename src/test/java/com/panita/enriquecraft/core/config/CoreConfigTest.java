package com.panita.enriquecraft.core.config;

import com.panita.enriquecraft.core.framework.config.ConfigIssue;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import com.panita.enriquecraft.core.message.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreConfigTest {

    @TempDir
    Path directory;

    private void writeConfig(String text) throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"), text);
    }

    @Test
    void defaultsAreValidAndUseTheSharedConstants() {
        ConfigManager manager = new ConfigManager(directory);

        CoreConfig config = manager.bind("core", CoreConfig.class);

        assertEquals(Messages.Prefix.DEFAULT, config.prefix.get());
        assertTrue(manager.reload().isClean(), "the file written from the defaults must load without issues");
    }

    @Test
    void blankPrefixIsRejected() throws IOException {
        writeConfig("{ \"core\": { \"messages\": { \"prefix\": \"  \" } } }");
        ConfigManager manager = new ConfigManager(directory);
        CoreConfig config = manager.bind("core", CoreConfig.class);

        ConfigReport report = manager.reload();

        assertEquals(Messages.Prefix.DEFAULT, config.prefix.get());
        assertEquals(List.of(new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, "core.messages.prefix", "must not be blank")),
                report.issues());
    }

    @Test
    void customPrefixIsUsed() throws IOException {
        writeConfig("{ \"core\": { \"messages\": { \"prefix\": \"<red>[Mine]</red>\" } } }");

        CoreConfig config = new ConfigManager(directory).bind("core", CoreConfig.class);

        assertEquals("<red>[Mine]</red>", config.prefix.get());
    }
}
