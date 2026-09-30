package com.panita.enriquecraft.core.framework.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
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
        JsonStore<List<String>> writer = new JsonStore<>("names.json", STRINGS, List.of());
        writer.load(world, JsonOps.INSTANCE);
        writer.set(List.of("uno"));
        WorldData data = new WorldData();
        JsonStore<List<String>> store = data.register("names.json", STRINGS, List.of());
        assertEquals(List.of(), store.get(), "nothing is read before a world starts");

        data.attach(world, JsonOps.INSTANCE);

        assertEquals(List.of("uno"), store.get());
    }

    @Test
    void registeringWhileAWorldRunsLoadsAtOnce() {
        WorldData data = new WorldData();
        data.attach(root, JsonOps.INSTANCE);

        JsonStore<List<String>> store = data.register("names.json", STRINGS, List.of("vacio"));

        assertEquals(List.of("vacio"), store.get());
        store.set(List.of("dos"));
        assertTrue(Files.exists(root.resolve("names.json")));
    }

    @Test
    void anotherWorldStartingReloadsTheRegisteredStores() {
        Path worldA = root.resolve("worldA");
        Path worldB = root.resolve("worldB");
        WorldData data = new WorldData();
        JsonStore<List<String>> store = data.register("names.json", STRINGS, List.of());
        data.attach(worldA, JsonOps.INSTANCE);
        store.set(List.of("solo en A"));
        data.detach();

        data.attach(worldB, JsonOps.INSTANCE);

        assertEquals(List.of(), store.get());
        data.detach();
        data.attach(worldA, JsonOps.INSTANCE);
        assertEquals(List.of("solo en A"), store.get());
    }

    @Test
    void openLoadsAStoreForAFileNotKnownUpFront() {
        WorldData data = new WorldData();
        data.attach(root, JsonOps.INSTANCE);
        data.open("players/one.json", STRINGS, List.of()).set(List.of("uno"));

        assertEquals(List.of("uno"), data.open("players/one.json", STRINGS, List.of()).get());
    }

    @Test
    void openWithoutAWorldIsAProgrammingError() {
        WorldData data = new WorldData();

        assertThrows(IllegalStateException.class, () -> data.open("players/one.json", STRINGS, List.of()));
    }
}
