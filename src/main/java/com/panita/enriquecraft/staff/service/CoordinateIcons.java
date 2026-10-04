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
                    // Navigation and markers
                    Items.COMPASS, Items.RECOVERY_COMPASS, Items.MAP, Items.ENDER_PEARL, Items.ENDER_EYE,
                    Items.LODESTONE, Items.BEACON, Items.NAME_TAG, Items.SPYGLASS, Items.CLOCK,
                    Items.LANTERN, Items.CAMPFIRE, Items.TORCH, Items.BELL, Items.REDSTONE_LAMP,
                    Items.RESPAWN_ANCHOR, Items.END_PORTAL_FRAME, Items.CONDUIT, Items.END_CRYSTAL, Items.SEA_LANTERN,
                    // Workstations and storage
                    Items.CRAFTING_TABLE, Items.ANVIL, Items.ENCHANTING_TABLE, Items.BOOKSHELF, Items.LECTERN,
                    Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.BREWING_STAND, Items.CAULDRON,
                    Items.LOOM, Items.STONECUTTER, Items.GRINDSTONE, Items.CARTOGRAPHY_TABLE, Items.FLETCHING_TABLE,
                    Items.SMITHING_TABLE, Items.COMPOSTER, Items.BARREL, Items.CHEST, Items.ENDER_CHEST,
                    Items.SHULKER_BOX, Items.BEEHIVE,
                    // Treasure
                    Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT, Items.COPPER_INGOT,
                    Items.NETHERITE_INGOT, Items.AMETHYST_SHARD, Items.ECHO_SHARD, Items.HEART_OF_THE_SEA, Items.NETHER_STAR,
                    Items.TOTEM_OF_UNDYING, Items.ELYTRA, Items.TRIDENT, Items.SHIELD, Items.BOW,
                    Items.CROSSBOW, Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.FISHING_ROD, Items.SADDLE,
                    Items.GOLDEN_APPLE, Items.DRAGON_EGG, Items.FIREWORK_ROCKET, Items.NAUTILUS_SHELL, Items.BLAZE_ROD,
                    // Landscape
                    Items.GRASS_BLOCK, Items.MYCELIUM, Items.MOSS_BLOCK, Items.PACKED_ICE, Items.OBSIDIAN,
                    Items.CRYING_OBSIDIAN, Items.GLOWSTONE, Items.OAK_SAPLING, Items.CHERRY_SAPLING, Items.CACTUS,
                    Items.PUMPKIN, Items.MELON, Items.SUNFLOWER, Items.POPPY, Items.LILAC,
                    // Danger
                    Items.TNT, Items.CREEPER_HEAD, Items.ZOMBIE_HEAD, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL);
        }
        return icons;
    }

    public static Item random(Random random) {
        List<Item> all = all();
        return all.get(random.nextInt(all.size()));
    }
}
