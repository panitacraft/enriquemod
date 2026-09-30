package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.core.framework.data.JsonStore;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The saved coordinates: which exist, and moving players to them. Names are unique ignoring case.
 */
public final class CoordinateService {

    public enum AddResult { ADDED, INVALID_NAME, DUPLICATE }

    public enum TeleportResult { TELEPORTED, DIMENSION_UNAVAILABLE }

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final JsonStore<List<SavedCoordinate>> store;

    public CoordinateService(WorldData worldData) {
        this.store = worldData.register("coordinates.json", SavedCoordinate.CODEC.listOf(), List.of());
    }

    /** Whether a name can be used: 1 to 32 letters, digits, underscores or dashes. */
    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    /** All coordinates, sorted by name. */
    public List<SavedCoordinate> all() {
        return store.get().stream()
                .sorted(Comparator.comparing(coordinate -> coordinate.name().toLowerCase()))
                .toList();
    }

    public List<String> names() {
        return all().stream().map(SavedCoordinate::name).toList();
    }

    public Optional<SavedCoordinate> find(String name) {
        return store.get().stream().filter(coordinate -> coordinate.name().equalsIgnoreCase(name)).findFirst();
    }

    public AddResult add(SavedCoordinate coordinate) {
        if (!isValidName(coordinate.name())) {
            return AddResult.INVALID_NAME;
        }
        if (find(coordinate.name()).isPresent()) {
            return AddResult.DUPLICATE;
        }
        List<SavedCoordinate> updated = new ArrayList<>(store.get());
        updated.add(coordinate);
        store.set(List.copyOf(updated));
        return AddResult.ADDED;
    }

    /** Removes a coordinate; returns whether there was one with that name. */
    public boolean remove(String name) {
        Optional<SavedCoordinate> existing = find(name);
        if (existing.isEmpty()) {
            return false;
        }
        List<SavedCoordinate> updated = new ArrayList<>(store.get());
        updated.remove(existing.get());
        store.set(List.copyOf(updated));
        return true;
    }

    public TeleportResult teleport(ServerPlayer player, SavedCoordinate coordinate) {
        ServerLevel level = player.level().getServer().getLevel(coordinate.dimension());
        if (level == null) {
            return TeleportResult.DIMENSION_UNAVAILABLE;
        }
        player.teleportTo(level, coordinate.x(), coordinate.y(), coordinate.z(), Set.<Relative>of(),
                coordinate.yaw(), coordinate.pitch(), true);
        return TeleportResult.TELEPORTED;
    }
}
