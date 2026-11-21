package com.panita.enriquecraft.staff.service;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs items into chest items that carry them, so a whole inventory can be handed over in a form
 * that cannot lose anything. A chest holds 27 stacks, so more items make more chests.
 */
public final class DeathChests {

    public static final int CHEST_SLOTS = 27;

    private DeathChests() {
    }

    /** Names a chest given its position among the chests being made. */
    @FunctionalInterface
    public interface Namer {

        Component name(int number, int total);
    }

    /**
     * Packs copies of the items into chest items, keeping their order. The given stacks are not
     * changed.
     *
     * @param items the stacks to pack; none of them empty
     * @return the chests, or none when there are no items
     */
    public static List<ItemStack> pack(List<ItemStack> items, Namer namer) {
        int total = (items.size() + CHEST_SLOTS - 1) / CHEST_SLOTS;
        List<ItemStack> chests = new ArrayList<>(total);
        for (int number = 1; number <= total; number++) {
            int start = (number - 1) * CHEST_SLOTS;
            List<ItemStack> contents = items.subList(start, Math.min(start + CHEST_SLOTS, items.size())).stream()
                    .map(ItemStack::copy)
                    .toList();
            ItemStack chest = new ItemStack(Items.CHEST);
            chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            chest.set(DataComponents.CUSTOM_NAME, namer.name(number, total));
            chests.add(chest);
        }
        return chests;
    }
}
