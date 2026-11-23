package com.panita.enriquecraft.qol.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.qol.config.QolConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareSoundsTest {

    @TempDir
    Path directory;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void aSoundOfTheGameIsValid() {
        assertTrue(ShareSounds.isValid("minecraft:entity.player.levelup"));
    }

    @Test
    void anUnknownSoundOrATextThatIsNotAnIdIsNotValid() {
        assertFalse(ShareSounds.isValid("minecraft:no.such.sound"));
        assertFalse(ShareSounds.isValid("Not An Id"));
        assertFalse(ShareSounds.isValid(""));
    }

    @Test
    void theDefaultsOfTheSettingsAreValidAndAnInvalidSoundFallsBackToTheDefault() throws Exception {
        java.nio.file.Files.writeString(directory.resolve("enriquecraft.json5"),
                "{ \"qol\": { \"shareCoordinates\": { \"soundPublic\": \"nope\" } } }");
        ConfigManager manager = new ConfigManager(directory);
        QolConfig config = manager.bind("qol", QolConfig.class);

        assertFalse(manager.reload().isClean());
        assertEquals("minecraft:entity.player.levelup", config.shareCoordinatesSoundPublic.get());
        assertEquals("minecraft:entity.player.levelup", config.shareCoordinatesSoundPrivate.get());
        assertTrue(config.shareCoordinatesEnabled.get());
    }
}
