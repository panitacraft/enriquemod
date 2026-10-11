package com.panita.enriquecraft.auth.service;

import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataMigrationTest {

    private static final UUID FROM = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TO = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @TempDir
    Path world;

    private Path playerData;
    private Path stats;
    private Path advancements;
    private Map<LevelResource, Path> folders;

    @BeforeEach
    void createFolders() throws IOException {
        playerData = Files.createDirectories(world.resolve("playerdata"));
        stats = Files.createDirectories(world.resolve("stats"));
        advancements = Files.createDirectories(world.resolve("advancements"));
        folders = Map.of(LevelResource.PLAYER_DATA_DIR, playerData, LevelResource.PLAYER_STATS_DIR, stats,
                LevelResource.PLAYER_ADVANCEMENTS_DIR, advancements);
    }

    private PlayerDataMigration.Result copy() {
        return new PlayerDataMigration().copy(folders::get, FROM, TO);
    }

    @Test
    void everythingTheOldUuidHasIsCopiedAndTheOriginalsStay() throws IOException {
        Files.writeString(playerData.resolve(FROM + ".dat"), "inventory");
        Files.writeString(stats.resolve(FROM + ".json"), "stats");
        Files.writeString(advancements.resolve(FROM + ".json"), "advancements");

        PlayerDataMigration.Result result = copy();

        assertEquals(3, result.copied());
        assertEquals(0, result.kept());
        assertEquals("inventory", Files.readString(playerData.resolve(TO + ".dat")));
        assertEquals("stats", Files.readString(stats.resolve(TO + ".json")));
        assertEquals("advancements", Files.readString(advancements.resolve(TO + ".json")));
        assertTrue(Files.exists(playerData.resolve(FROM + ".dat")));
    }

    @Test
    void whatTheNewUuidAlreadyHasIsNeverOverwritten() throws IOException {
        Files.writeString(playerData.resolve(FROM + ".dat"), "old");
        Files.writeString(playerData.resolve(TO + ".dat"), "new");

        PlayerDataMigration.Result result = copy();

        assertEquals(0, result.copied());
        assertEquals(1, result.kept());
        assertEquals("new", Files.readString(playerData.resolve(TO + ".dat")));
    }

    @Test
    void filesThatDoNotExistAreSkipped() throws IOException {
        Files.writeString(stats.resolve(FROM + ".json"), "stats");

        PlayerDataMigration.Result result = copy();

        assertEquals(1, result.copied());
        assertFalse(Files.exists(playerData.resolve(TO + ".dat")));
    }

    @Test
    void aPlayerWithNothingCopiesNothing() {
        assertEquals(new PlayerDataMigration.Result(0, 0), copy());
    }
}
