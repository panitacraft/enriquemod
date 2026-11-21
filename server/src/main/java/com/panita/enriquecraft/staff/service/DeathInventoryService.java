package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.core.framework.data.SnbtStore;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.item.ItemGiving;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.data.DeathPlayer;
import com.panita.enriquecraft.staff.data.DeathRecord;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The death inventories of every player, kept in one file per player, newest first, and trimmed
 * to the configured maximum. Files are read when needed, since deaths are rare.
 */
public final class DeathInventoryService {

    private static final String DIRECTORY = "death_inventories/";
    private static final String EXTENSION = ".snbt";

    private final WorldData worldData;
    private final StaffConfig config;

    public DeathInventoryService(WorldData worldData, StaffConfig config) {
        this.worldData = worldData;
        this.config = config;
    }

    /** Every player with at least one kept death, the one who died most recently first. */
    public List<DeathPlayer> playersWithDeaths() {
        List<DeathPlayer> players = new ArrayList<>();
        for (String file : worldData.fileNames(DIRECTORY)) {
            if (!file.endsWith(EXTENSION)) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(file.substring(0, file.length() - EXTENSION.length()));
            } catch (IllegalArgumentException e) {
                continue;
            }
            List<DeathRecord> records = records(id);
            if (!records.isEmpty()) {
                DeathRecord newest = records.getFirst();
                players.add(new DeathPlayer(id, newest.playerName(), newest.diedAt(), records.size()));
            }
        }
        players.sort(Comparator.comparing(DeathPlayer::lastDeath).reversed());
        return List.copyOf(players);
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

    /**
     * Gives a staff member the whole inventory packed into chests.
     *
     * @return how many chests they received
     */
    public int giveChests(ServerPlayer staff, DeathRecord record, DeathChests.Namer namer) {
        List<ItemStack> chests = DeathChests.pack(record.nonEmptyItems(), namer);
        chests.forEach(chest -> ItemGiving.give(staff, chest));
        return chests.size();
    }

    /**
     * Gives a player back the items of a death inventory. Each stack returns to the slot it came
     * from (armor is worn again) when that slot is free, which is the case after a respawn; a stack
     * whose slot is taken is given like a picked-up item instead, so nothing is lost or overwritten.
     *
     * The death is then marked as restored, so staff can tell at a glance, but it stays available to inspect.
     *
     * @return how many stacks were given
     */
    public int restore(ServerPlayer target, DeathRecord record, Instant now) {
        int given = 0;
        for (int slot = 0; slot < record.items().size(); slot++) {
            ItemStack stack = record.items().get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (target.getInventory().getItem(slot).isEmpty()) {
                target.getInventory().setItem(slot, stack.copy());
            } else {
                ItemGiving.give(target, stack);
            }
            given++;
        }
        replace(record.player(), record.markRestored(now));
        return given;
    }

    /** One record of a player, by id. */
    public Optional<DeathRecord> find(UUID player, UUID recordId) {
        return records(player).stream().filter(record -> record.id().equals(recordId)).findFirst();
    }

    private void replace(UUID player, DeathRecord changed) {
        SnbtStore<List<DeathRecord>> store = open(player);
        store.set(store.get().stream().map(record -> record.id().equals(changed.id()) ? changed : record).toList());
    }

    /** Keeps the first {@code max} of a newest-first list. */
    static List<DeathRecord> trim(List<DeathRecord> newestFirst, int max) {
        return newestFirst.size() <= max ? newestFirst : newestFirst.subList(0, max);
    }

    private SnbtStore<List<DeathRecord>> open(UUID player) {
        return worldData.open(DIRECTORY + player + EXTENSION, DeathRecord.CODEC.listOf(), List.of());
    }
}
