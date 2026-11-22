package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.moderation.config.ModerationConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientModPolicyTest {

    @TempDir
    Path directory;

    private ModerationConfig config;
    private ClientModPolicy policy;

    @BeforeEach
    void setUp() {
        ConfigManager manager = new ConfigManager(directory);
        config = manager.bind("moderation", ModerationConfig.class);
        policy = new ClientModPolicy(config);
        assertTrue(manager.reload().isClean());
    }

    @Test
    void theWorldIdChannelOfJourneyMapAndVoxelMapIsBlocked() {
        List<Identifier> announced = List.of(Identifier.parse("fabric:registry/sync"), Identifier.parse("worldinfo:world_id"));

        assertEquals(Optional.of("JourneyMap o VoxelMap"), policy.blockedMod(announced));
    }

    @Test
    void theChannelsOfXaerosMapsAreAllowed() {
        List<Identifier> announced = List.of(Identifier.parse("xaerominimap:main"), Identifier.parse("xaeroworldmap:main"));

        assertEquals(Optional.empty(), policy.blockedMod(announced));
    }

    @Test
    void nothingIsBlockedWhenTheCheckIsOff() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"), "{ \"moderation\": { \"clientMods\": { \"enabled\": false } } }");
        ConfigManager manager = new ConfigManager(directory);
        ClientModPolicy off = new ClientModPolicy(manager.bind("moderation", ModerationConfig.class));
        manager.reload();

        assertEquals(Optional.empty(), off.blockedMod(List.of(Identifier.parse("worldinfo:world_id"))));
    }
}
