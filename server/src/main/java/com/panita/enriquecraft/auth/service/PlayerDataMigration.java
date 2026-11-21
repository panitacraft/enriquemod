package com.panita.enriquecraft.auth.service;

import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Carries what the game keeps of a player under their UUID (inventory and position, statistics, advancements)
 * over to another UUID. A player who changes how they log in gets another UUID, and would otherwise start from
 * nothing. Files are copied, never moved, and files the new UUID already has are left alone.
 */
public final class PlayerDataMigration {

    /** A folder of the world, and the extension of the files in it. */
    private record Kind(LevelResource folder, String extension) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataMigration.class);
    private static final List<Kind> KINDS = List.of(
            new Kind(LevelResource.PLAYER_DATA_DIR, ".dat"),
            new Kind(LevelResource.PLAYER_STATS_DIR, ".json"),
            new Kind(LevelResource.PLAYER_ADVANCEMENTS_DIR, ".json"));

    /** How many files were copied and how many were kept apart because the new UUID already had its own. */
    public record Result(int copied, int kept) {
    }

    /**
     * Copies the data of a player to a new UUID. The player must be offline: the game saves its own copy when
     * they leave and would overwrite what was copied.
     */
    public Result copy(Function<LevelResource, Path> worldPath, UUID from, UUID to) {
        int copied = 0;
        int kept = 0;
        for (Kind kind : KINDS) {
            Path folder = worldPath.apply(kind.folder());
            Path source = folder.resolve(from + kind.extension());
            Path target = folder.resolve(to + kind.extension());
            if (!Files.exists(source)) {
                continue;
            }
            if (Files.exists(target)) {
                kept++;
                continue;
            }
            try {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                copied++;
            } catch (IOException e) {
                LOGGER.error("Could not copy {} to {}: {}", source, target, e.getMessage());
            }
        }
        return new Result(copied, kept);
    }
}
