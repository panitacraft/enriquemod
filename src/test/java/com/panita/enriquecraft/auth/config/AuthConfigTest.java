package com.panita.enriquecraft.auth.config;

import com.panita.enriquecraft.core.framework.config.ConfigIssue;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthConfigTest {

    @TempDir
    Path directory;

    @Test
    void defaultsMatchTheAgreedBehavior() {
        ConfigManager manager = new ConfigManager(directory);

        AuthConfig config = manager.bind("auth", AuthConfig.class);

        assertEquals(4, config.pinLength.get());
        assertEquals(3, config.maxAttempts.get());
        assertEquals(24, config.sessionHours.get());
        assertEquals(3, config.kicksBeforeBlock.get());
        assertTrue(manager.reload().isClean());
    }

    @Test
    void pinLengthAcceptsFourToSixOnly() throws IOException {
        for (String value : List.of("3", "7", "0", "\"four\"")) {
            Files.writeString(directory.resolve("enriquecraft.json5"),
                    "{ \"auth\": { \"pin\": { \"length\": " + value + " } } }");
            ConfigManager manager = new ConfigManager(directory);
            AuthConfig config = manager.bind("auth", AuthConfig.class);

            ConfigReport report = manager.reload();

            assertEquals(4, config.pinLength.get(), value);
            assertEquals(ConfigIssue.Kind.INVALID_VALUE, report.issues().get(0).kind(), value);
        }
        Files.writeString(directory.resolve("enriquecraft.json5"), "{ \"auth\": { \"pin\": { \"length\": 6 } } }");
        assertEquals(6, new ConfigManager(directory).bind("auth", AuthConfig.class).pinLength.get());
    }
}
