package com.panita.enriquecraft.link.config;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkConfigTest {

    @TempDir
    Path directory;

    @Test
    void defaultsMatchTheAgreedBehavior() {
        ConfigManager manager = new ConfigManager(directory);

        LinkConfig config = manager.bind("link", LinkConfig.class);

        assertEquals("https://api.panitacraft.com", config.apiUrl.get());
        assertEquals(10, config.requestTimeoutSeconds.get());
        assertEquals(50, config.maxRemovalPercent.get());
        assertTrue(manager.reload().isClean());
    }

    @Test
    void theAddressMustBeAnHttpAddressWithoutATrailingSlash() throws IOException {
        for (String bad : new String[]{"api.panitacraft.com", "ftp://api.panitacraft.com", "https://api.panitacraft.com/", "https://"}) {
            Files.writeString(directory.resolve("enriquecraft.json5"), "{ \"link\": { \"apiUrl\": \"" + bad + "\" } }");
            ConfigManager manager = new ConfigManager(directory);
            LinkConfig config = manager.bind("link", LinkConfig.class);

            assertEquals("https://api.panitacraft.com", config.apiUrl.get(), bad);
            assertFalse(manager.reload().isClean(), bad);
        }
    }

    @Test
    void aLocalAddressIsAccepted() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"), "{ \"link\": { \"apiUrl\": \"http://127.0.0.1:8080\" } }");
        ConfigManager manager = new ConfigManager(directory);

        assertEquals("http://127.0.0.1:8080", manager.bind("link", LinkConfig.class).apiUrl.get());
    }
}
