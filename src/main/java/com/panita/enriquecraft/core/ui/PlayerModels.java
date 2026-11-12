package com.panita.enriquecraft.core.ui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * A head item that a client companion draws as the whole player instead, in the screen of that player's own profile. The client draws
 * the player it is, with the skin the server gave them, so it follows SkinsRestorer without the server sending a skin. Every other
 * client, and a chest, draws the head.
 */
public final class PlayerModels {

    /** The key in the custom data of an item that asks for the model of the player who views it. */
    public static final String KEY = "enriquecraft_model";

    private PlayerModels() {
    }

    /** The head of a player, marked so the companion shows the model of whoever views it. Use it only in the viewer's own screen. */
    public static ItemStack ofViewer(ItemStack head) {
        CompoundTag mark = new CompoundTag();
        mark.putBoolean(KEY, true);
        head.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        return head;
    }

    /** Whether the item asks for the model of the viewer. */
    public static boolean isModel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(Items.PLAYER_HEAD) && data != null && data.copyTag().getBooleanOr(KEY, false);
    }
}
