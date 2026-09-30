package com.panita.enriquecraft.staff.service;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

/**
 * The items a saved coordinate can be shown as in menus. One is picked at random when a
 * coordinate is saved, and then kept.
 */
public final class CoordinateIcons {

    private static List<Item> icons;

    private CoordinateIcons() {
    }

    /** The available icons. Built on first use, because items exist only once Minecraft has started. */
    static List<Item> all() {
        if (icons == null) {
            icons = List.of(
                    Items.COMPASS, Items.RECOVERY_COMPASS, Items.MAP, Items.ENDER_PEARL, Items.ENDER_EYE,
                    Items.LODESTONE, Items.BEACON, Items.NAME_TAG, Items.SPYGLASS, Items.CLOCK,
                    Items.LANTERN, Items.CAMPFIRE, Items.CRAFTING_TABLE, Items.ANVIL, Items.ENCHANTING_TABLE,
                    Items.BELL, Items.BOOKSHELF, Items.LECTERN, Items.TORCH, Items.DIAMOND,
                    Items.EMERALD, Items.HEART_OF_THE_SEA, Items.NETHER_STAR, Items.TOTEM_OF_UNDYING);
        }
        return icons;
    }

    public static Item random(Random random) {
        List<Item> all = all();
        return all.get(random.nextInt(all.size()));
    }
}
