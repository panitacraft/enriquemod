package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.core.framework.data.SnbtStore;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.staff.data.PlayerRef;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The saved coordinates: which exist, and moving players to them. Names are unique ignoring case.
 */
public final class CoordinateService {

    public enum AddResult { ADDED, INVALID_NAME, DUPLICATE }

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final SnbtStore<List<SavedCoordinate>> store;

    public CoordinateService(WorldData worldData) {
        this.store = worldData.register("coordinates.snbt", SavedCoordinate.CODEC.listOf(), List.of());
    }

    /** Whether a name can be used: 1 to 32 letters, digits, underscores or dashes. */
    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    /** All coordinates in the order staff arranged them; a new one goes last. */
    public List<SavedCoordinate> all() {
        return store.get();
    }

    /** The names, alphabetically, for suggestions. */
    public List<String> names() {
        return all().stream().map(SavedCoordinate::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
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

    /** Changes the item a coordinate is shown as; returns whether there was a coordinate with that name. */
    public boolean updateIcon(String name, Item icon) {
        Optional<SavedCoordinate> existing = find(name);
        if (existing.isEmpty()) {
            return false;
        }
        List<SavedCoordinate> updated = new ArrayList<>(store.get());
        updated.set(updated.indexOf(existing.get()), existing.get().withIcon(icon));
        store.set(List.copyOf(updated));
        return true;
    }

    /**
     * Shows a coordinate as the head of a player.
     *
     * @return whether there is a coordinate with that name
     */
    public boolean updateIconHead(String name, PlayerRef owner) {
        Optional<SavedCoordinate> existing = find(name);
        if (existing.isEmpty()) {
            return false;
        }
        List<SavedCoordinate> updated = new ArrayList<>(store.get());
        updated.set(updated.indexOf(existing.get()), existing.get().withHeadOf(owner));
        store.set(List.copyOf(updated));
        return true;
    }

    /**
     * Moves a coordinate to the place of another, which shifts the ones in between by one. This order is what
     * every menu shows, for all staff.
     *
     * @return whether both exist and differ
     */
    public boolean move(String name, String ontoName) {
        Optional<SavedCoordinate> moved = find(name);
        Optional<SavedCoordinate> onto = find(ontoName);
        if (moved.isEmpty() || onto.isEmpty() || moved.get().equals(onto.get())) {
            return false;
        }
        List<SavedCoordinate> updated = new ArrayList<>(store.get());
        int target = updated.indexOf(onto.get());
        updated.remove(moved.get());
        updated.add(target, moved.get());
        store.set(List.copyOf(updated));
        return true;
    }

    /**
     * Gives a coordinate another display name, which does not change its id.
     *
     * @return whether the text is a valid display name and there is a coordinate with that name
     */
    public boolean updateDisplayName(String name, String displayName) {
        String trimmed = displayName.trim();
        Optional<SavedCoordinate> existing = find(name);
        if (existing.isEmpty() || !SavedCoordinate.isValidDisplayName(trimmed)) {
            return false;
        }
        List<SavedCoordinate> updated = new ArrayList<>(store.get());
        updated.set(updated.indexOf(existing.get()), existing.get().withDisplayName(trimmed));
        store.set(List.copyOf(updated));
        return true;
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

    public Teleporter.Result teleport(ServerPlayer player, SavedCoordinate coordinate) {
        return Teleporter.teleport(player, coordinate.dimension(), coordinate.x(), coordinate.y(), coordinate.z(),
                coordinate.yaw(), coordinate.pitch());
    }
}
