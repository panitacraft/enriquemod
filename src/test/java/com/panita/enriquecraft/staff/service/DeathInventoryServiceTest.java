package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.data.DeathRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathInventoryServiceTest {

    private static final UUID ANA = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID BEA = UUID.fromString("66666666-7777-8888-9999-000000000000");

    @TempDir
    Path directory;

    private WorldData worldData;

    @BeforeEach
    void attachWorld() {
        worldData = new WorldData();
        worldData.attach(directory.resolve("world"), MinecraftTestSupport.ops());
    }

    private DeathInventoryService service(int max) throws IOException {
        Path configDirectory = directory.resolve("config" + max);
        Files.createDirectories(configDirectory);
        Files.writeString(configDirectory.resolve("enriquecraft.json5"),
                "{ \"staff\": { \"deathRecords\": { \"maxPerPlayer\": " + max + " } } }");
        StaffConfig config = new ConfigManager(configDirectory).bind("staff", StaffConfig.class);
        return new DeathInventoryService(worldData, config);
    }

    private static DeathRecord record(UUID player, int minute) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            items.add(slot == 0 ? new ItemStack(Items.DIAMOND, minute) : ItemStack.EMPTY);
        }
        return new DeathRecord(UUID.randomUUID(), player, "Jugador", Instant.parse("2026-09-30T04:00:00Z").plusSeconds(minute * 60L),
                Level.OVERWORLD, 1, 2, 3, "cause " + minute, 5, items);
    }

    @Test
    void recordsAreKeptNewestFirst() throws IOException {
        DeathInventoryService service = service(20);
        DeathRecord first = record(ANA, 1);
        DeathRecord second = record(ANA, 2);
        DeathRecord third = record(ANA, 3);

        service.add(first);
        service.add(second);
        service.add(third);

        assertEquals(List.of(third.id(), second.id(), first.id()),
                service.records(ANA).stream().map(DeathRecord::id).toList());
    }

    @Test
    void playersHaveSeparateHistories() throws IOException {
        DeathInventoryService service = service(20);

        service.add(record(ANA, 1));
        service.add(record(BEA, 2));
        service.add(record(BEA, 3));

        assertEquals(1, service.records(ANA).size());
        assertEquals(2, service.records(BEA).size());
        assertTrue(Files.exists(directory.resolve("world/death_inventories/" + ANA + ".snbt")));
        assertTrue(Files.exists(directory.resolve("world/death_inventories/" + BEA + ".snbt")));
    }

    @Test
    void noRecordsGivesAnEmptyListAndCreatesNoFile() throws IOException {
        DeathInventoryService service = service(20);

        assertEquals(List.of(), service.records(ANA));
        assertFalse(Files.exists(directory.resolve("world/death_inventories/" + ANA + ".snbt")));
    }

    @Test
    void theOldestRecordsAreDeletedBeyondTheMaximum() throws IOException {
        DeathInventoryService service = service(3);
        List<DeathRecord> made = new ArrayList<>();
        for (int minute = 1; minute <= 5; minute++) {
            DeathRecord record = record(ANA, minute);
            made.add(record);
            service.add(record);
        }

        assertEquals(List.of(made.get(4).id(), made.get(3).id(), made.get(2).id()),
                service.records(ANA).stream().map(DeathRecord::id).toList());
    }

    @Test
    void theMaximumAppliesPerPlayer() throws IOException {
        DeathInventoryService service = service(2);
        service.add(record(ANA, 1));
        service.add(record(ANA, 2));
        service.add(record(ANA, 3));

        service.add(record(BEA, 4));

        assertEquals(2, service.records(ANA).size());
        assertEquals(1, service.records(BEA).size());
    }

    @Test
    void deleteRemovesOnlyThatRecord() throws IOException {
        DeathInventoryService service = service(20);
        DeathRecord keep = record(ANA, 1);
        DeathRecord remove = record(ANA, 2);
        service.add(keep);
        service.add(remove);

        assertTrue(service.delete(ANA, remove.id()));

        assertEquals(List.of(keep.id()), service.records(ANA).stream().map(DeathRecord::id).toList());
    }

    @Test
    void deletingAnUnknownRecordReportsIt() throws IOException {
        DeathInventoryService service = service(20);
        service.add(record(ANA, 1));

        assertFalse(service.delete(ANA, UUID.randomUUID()));
        assertFalse(service.delete(BEA, UUID.randomUUID()));
        assertEquals(1, service.records(ANA).size());
    }

    @Test
    void recordsSurviveARestartExactly() throws IOException {
        DeathInventoryService service = service(20);
        DeathRecord saved = record(ANA, 7);
        service.add(saved);

        WorldData restarted = new WorldData();
        restarted.attach(directory.resolve("world"), MinecraftTestSupport.ops());
        DeathInventoryService reloaded = new DeathInventoryService(restarted,
                new ConfigManager(directory.resolve("config20")).bind("staff", StaffConfig.class));

        DeathRecord loaded = reloaded.records(ANA).get(0);
        assertEquals(saved.id(), loaded.id());
        assertTrue(ItemStack.matches(saved.items().get(0), loaded.items().get(0)));
        assertEquals("cause 7", loaded.cause());
    }

    @Test
    void trimKeepsTheFirstEntriesOfANewestFirstList() {
        List<DeathRecord> records = List.of(record(ANA, 3), record(ANA, 2), record(ANA, 1));

        assertEquals(records.subList(0, 2), DeathInventoryService.trim(records, 2));
        assertEquals(records, DeathInventoryService.trim(records, 3));
        assertEquals(records, DeathInventoryService.trim(records, 50));
    }
}
