package com.panita.enriquecraft.core.framework.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldDataTest {

    private static final Codec<List<String>> STRINGS = Codec.STRING.listOf();

    @TempDir
    Path root;

    @Test
    void registeredStoresLoadWhenAWorldStarts() {
        Path world = root.resolve("worldA");
        SnbtStore<List<String>> writer = new SnbtStore<>("names.snbt", STRINGS, List.of());
        writer.load(world, NbtOps.INSTANCE);
        writer.set(List.of("uno"));
        WorldData data = new WorldData();
        SnbtStore<List<String>> store = data.register("names.snbt", STRINGS, List.of());
        assertEquals(List.of(), store.get(), "nothing is read before a world starts");

        data.attach(world, NbtOps.INSTANCE);

        assertEquals(List.of("uno"), store.get());
    }

    @Test
    void registeringWhileAWorldRunsLoadsAtOnce() {
        WorldData data = new WorldData();
        data.attach(root, NbtOps.INSTANCE);

        SnbtStore<List<String>> store = data.register("names.snbt", STRINGS, List.of("vacio"));

        assertEquals(List.of("vacio"), store.get());
        store.set(List.of("dos"));
        assertTrue(Files.exists(root.resolve("names.snbt")));
    }

    @Test
    void anotherWorldStartingReloadsTheRegisteredStores() {
        Path worldA = root.resolve("worldA");
        Path worldB = root.resolve("worldB");
        WorldData data = new WorldData();
        SnbtStore<List<String>> store = data.register("names.snbt", STRINGS, List.of());
        data.attach(worldA, NbtOps.INSTANCE);
        store.set(List.of("solo en A"));
        data.detach();

        data.attach(worldB, NbtOps.INSTANCE);

        assertEquals(List.of(), store.get());
        data.detach();
        data.attach(worldA, NbtOps.INSTANCE);
        assertEquals(List.of("solo en A"), store.get());
    }

    @Test
    void openLoadsAStoreForAFileNotKnownUpFront() {
        WorldData data = new WorldData();
        data.attach(root, NbtOps.INSTANCE);
        data.open("players/one.snbt", STRINGS, List.of()).set(List.of("uno"));

        assertEquals(List.of("uno"), data.open("players/one.snbt", STRINGS, List.of()).get());
    }

    @Test
    void openWithoutAWorldIsAProgrammingError() {
        WorldData data = new WorldData();

        assertThrows(IllegalStateException.class, () -> data.open("players/one.snbt", STRINGS, List.of()));
    }
}
