package com.panita.enriquecraft.core.framework.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The data files of the mod for the world that is currently running. Files live in the world
 * folder, under {@code enriquecraft/}, so they travel with the world save and its backups.
 * <p>
 * Modules register their stores while the mod starts, before any world exists. Every store is
 * loaded when a world starts ({@link #attach}), and loaded again if another world starts later in
 * the same session.
 */
public final class WorldData {

    private final List<JsonStore<?>> registered = new ArrayList<>();
    private Path directory;
    private DynamicOps<JsonElement> ops;

    /**
     * Registers a store that lives as long as the mod. It is loaded now if a world is running,
     * and on every later world start.
     */
    public <T> JsonStore<T> register(String relativePath, Codec<T> codec, T emptyValue) {
        JsonStore<T> store = new JsonStore<>(relativePath, codec, emptyValue);
        registered.add(store);
        if (directory != null) {
            store.load(directory, ops);
        }
        return store;
    }

    /**
     * Creates and loads a store for a file that is not known up front, such as one per player.
     * The caller keeps it; it is not reloaded when another world starts.
     *
     * @throws IllegalStateException if no world is running
     */
    public <T> JsonStore<T> open(String relativePath, Codec<T> codec, T emptyValue) {
        if (directory == null) {
            throw new IllegalStateException("No world is running, so " + relativePath + " cannot be opened");
        }
        JsonStore<T> store = new JsonStore<>(relativePath, codec, emptyValue);
        store.load(directory, ops);
        return store;
    }

    /**
     * Called when a world starts.
     *
     * @param directory the folder for the mod's data in that world
     * @param ops       how to turn data into JSON, with that world's registries
     */
    public void attach(Path directory, DynamicOps<JsonElement> ops) {
        this.directory = directory;
        this.ops = ops;
        registered.forEach(store -> store.load(directory, ops));
    }

    /** Called when the world stops. */
    public void detach() {
        directory = null;
        ops = null;
    }
}
