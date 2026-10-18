package com.panita.enriquecraft.discord.config;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordConfigTest {

    @TempDir
    Path directory;

    @Test
    void defaultsAreEnabledWithNoAddress() {
        ConfigManager manager = new ConfigManager(directory);

        DiscordConfig config = manager.bind("discord", DiscordConfig.class);

        assertTrue(config.enabled.get());
        assertEquals("", config.url.get());
        assertEquals("Discord", config.deliveryTag.get());
        assertTrue(config.mediaEnabled.get());
        assertEquals("cdn.discordapp.com,media.discordapp.net", config.mediaHosts.get());
        assertTrue(manager.reload().isClean());
    }

    @Test
    void anInvalidListOfMediaHostsInTheFileIsReportedAndTheDefaultIsUsed() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"), "{ \"discord\": { \"mediaHosts\": \"https://cdn.discordapp.com/\" } }");
        ConfigManager manager = new ConfigManager(directory);

        DiscordConfig config = manager.bind("discord", DiscordConfig.class);

        assertEquals("cdn.discordapp.com,media.discordapp.net", config.mediaHosts.get());
        assertFalse(manager.reload().isClean());
    }

    @Test
    void acceptsASecureAddressAndALocalPlainOne() {
        assertTrue(DiscordConfig.isBridgeUrl(""));
        assertTrue(DiscordConfig.isBridgeUrl("wss://bot.example.com/bridge"));
        assertTrue(DiscordConfig.isBridgeUrl("ws://localhost:3005/bridge"));
        assertTrue(DiscordConfig.isBridgeUrl("ws://127.0.0.1:3005/bridge"));
    }

    @Test
    void refusesAPlainAddressOfAnotherHostAndAnythingThatIsNotAnAddress() {
        assertFalse(DiscordConfig.isBridgeUrl("ws://bot.example.com/bridge"));
        assertFalse(DiscordConfig.isBridgeUrl("https://bot.example.com/bridge"));
        assertFalse(DiscordConfig.isBridgeUrl("bot.example.com"));
        assertFalse(DiscordConfig.isBridgeUrl("wss://"));
        assertFalse(DiscordConfig.isBridgeUrl("not a url"));
    }

    @Test
    void anInvalidAddressInTheFileIsReportedAndTheDefaultIsUsed() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"), "{ \"discord\": { \"url\": \"ws://bot.example.com/bridge\" } }");
        ConfigManager manager = new ConfigManager(directory);

        DiscordConfig config = manager.bind("discord", DiscordConfig.class);

        assertEquals("", config.url.get());
        assertFalse(manager.reload().isClean());
    }
}
