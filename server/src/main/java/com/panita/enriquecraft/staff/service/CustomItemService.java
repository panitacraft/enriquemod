package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.core.framework.data.SnbtStore;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.item.CustomItemTag;
import com.panita.enriquecraft.staff.data.SavedItem;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The saved custom items. Names are lowercase and unique; lookups ignore the case of the input.
 */
public final class CustomItemService {

    public enum AddResult { ADDED, INVALID_NAME, DUPLICATE }

    private final SnbtStore<List<SavedItem>> store;

    public CustomItemService(WorldData worldData) {
        this.store = worldData.register("custom_items.snbt", SavedItem.CODEC.listOf(), List.of());
    }

    /** All items, sorted by name. */
    public List<SavedItem> all() {
        return store.get().stream().sorted(Comparator.comparing(SavedItem::name)).toList();
    }

    public List<String> names() {
        return all().stream().map(SavedItem::name).toList();
    }

    public Optional<SavedItem> find(String name) {
        String lookup = name.toLowerCase(Locale.ROOT);
        return store.get().stream().filter(item -> item.name().equals(lookup)).findFirst();
    }

    /**
     * Saves an item as a custom item. On success the given stack is also marked with the custom
     * item tag, so the item in the staff member's hand matches the saved one; a rejected item is
     * left untouched.
     *
     * @param held the item to save, as held by the staff member
     */
    public AddResult save(ItemStack held, String name, UUID savedBy, String savedByName, Instant savedAt) {
        if (!CustomItemTag.isValidName(name)) {
            return AddResult.INVALID_NAME;
        }
        if (find(name).isPresent()) {
            return AddResult.DUPLICATE;
        }
        CustomItemTag.apply(held, name);
        List<SavedItem> updated = new ArrayList<>(store.get());
        updated.add(new SavedItem(name, held.copy(), savedBy, savedByName, savedAt));
        store.set(List.copyOf(updated));
        return AddResult.ADDED;
    }

    /** Removes an item; returns whether there was one with that name. */
    public boolean remove(String name) {
        Optional<SavedItem> existing = find(name);
        if (existing.isEmpty()) {
            return false;
        }
        List<SavedItem> updated = new ArrayList<>(store.get());
        updated.remove(existing.get());
        store.set(List.copyOf(updated));
        return true;
    }
}
