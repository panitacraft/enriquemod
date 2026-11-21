package com.panita.enriquecraft.core.ui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * A small picture of an item inside a line of text, to tell lines apart at a glance. Anything that shows the
 * line draws the picture from the game's own item textures, so it needs nothing from the client companion.
 */
public final class ItemIcons {

    private ItemIcons() {
    }

    /** The item's picture followed by a space, to put in front of a line. */
    public static Component of(Item item) {
        Identifier sprite = Identifier.fromNamespaceAndPath("minecraft", "item/" + texture(item));
        return Component.object(new AtlasSprite(AtlasIds.ITEMS, sprite)).append(Component.literal(" "));
    }

    /**
     * The texture an item is drawn with, which is its own name except for items the game draws with another's
     * texture: the enchanted golden apple is a golden apple with the enchantment glint over it.
     */
    private static String texture(Item item) {
        if (item == Items.ENCHANTED_GOLDEN_APPLE) {
            return "golden_apple";
        }
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
