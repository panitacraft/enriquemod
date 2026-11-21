package com.panita.enriquecraft.client.chat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The player's own choice about images in the chat, kept in {@code config/enriquecraft-client.properties}. The client fetches
 * the image itself from the host Discord uses, which sees the player's address, so the player can turn it off with
 * {@code chatImages=false}. A missing file or key means on.
 */
final class ChatImageSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatImageSettings.class);
    private static final String FILE = "enriquecraft-client.properties";
    private static final String KEY = "chatImages";

    private ChatImageSettings() {
    }

    /** Read at each use, so the player can change it without restarting the game. */
    static boolean enabled() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return true;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.warn("Could not read {}: {}", FILE, e.getMessage());
            return true;
        }
        return !"false".equalsIgnoreCase(properties.getProperty(KEY, "true").strip());
    }
}
