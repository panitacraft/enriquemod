package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.core.framework.data.SnbtStore;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.data.DeathRecord;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The death inventories of every player, kept in one file per player, newest first, and trimmed
 * to the configured maximum. Files are read when needed, since deaths are rare.
 */
public final class DeathInventoryService {

    private static final String DIRECTORY = "death_inventories/";

    private final WorldData worldData;
    private final StaffConfig config;

    public DeathInventoryService(WorldData worldData, StaffConfig config) {
        this.worldData = worldData;
        this.config = config;
    }

    /** A player's death inventories, newest first. */
    public List<DeathRecord> records(UUID player) {
        return open(player).get();
    }

    /**
     * Records the inventory of a player who is dying. Nothing is recorded when the inventory is
     * empty.
     *
     * @return whether a record was made
     */
    public boolean capture(ServerPlayer player, DamageSource source) {
        DeathRecord record = DeathRecord.of(player, source, Instant.now());
        if (!record.hasItems()) {
            return false;
        }
        add(record);
        return true;
    }

    /** Adds a record as the newest one and deletes the oldest ones beyond the configured maximum. */
    public void add(DeathRecord record) {
        SnbtStore<List<DeathRecord>> store = open(record.player());
        List<DeathRecord> updated = new ArrayList<>(store.get());
        updated.add(0, record);
        store.set(List.copyOf(trim(updated, config.maxDeathRecordsPerPlayer.get())));
    }

    /** Deletes a record; returns whether there was one with that id. */
    public boolean delete(UUID player, UUID recordId) {
        SnbtStore<List<DeathRecord>> store = open(player);
        List<DeathRecord> updated = new ArrayList<>(store.get());
        if (!updated.removeIf(record -> record.id().equals(recordId))) {
            return false;
        }
        store.set(List.copyOf(updated));
        return true;
    }

    /** Keeps the first {@code max} of a newest-first list. */
    static List<DeathRecord> trim(List<DeathRecord> newestFirst, int max) {
        return newestFirst.size() <= max ? newestFirst : newestFirst.subList(0, max);
    }

    private SnbtStore<List<DeathRecord>> open(UUID player) {
        return worldData.open(DIRECTORY + player + ".snbt", DeathRecord.CODEC.listOf(), List.of());
    }
}
