package com.panita.enriquecraft.core.ui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * A head item that a client companion draws as the whole player instead. In the screen of the viewer's own profile the client draws the
 * player it is, with the skin the server gave them. In the screen of another player's profile the head carries that player's profile,
 * with the skin the server shows them (SkinsRestorer puts it there), and the client draws a player with that skin. Either way the server
 * sends no skin of its own. Every other client, and a chest, draws the head.
 */
public final class PlayerModels {

    /** The key in the custom data of an item that asks for the model of the player who views it. */
    public static final String KEY = "enriquecraft_model";

    /** The key in the custom data of an item that asks for the model of the player whose profile the head holds. */
    public static final String PLAYER_KEY = "enriquecraft_model_of_player";

    private PlayerModels() {
    }

    /** The head of another player, marked so the companion shows a model with the skin in the head's profile. */
    public static ItemStack ofPlayer(ItemStack head) {
        CompoundTag mark = new CompoundTag();
        mark.putBoolean(PLAYER_KEY, true);
        head.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        return head;
    }

    /** Whether the item asks for the model of the viewer, which is drawn from the player the client is. */
    public static boolean isViewer(ItemStack stack) {
        return hasMark(stack, KEY);
    }

    private static boolean hasMark(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(Items.PLAYER_HEAD) && data != null && data.copyTag().getBooleanOr(key, false);
    }

    /** The head of a player, marked so the companion shows the model of whoever views it. Use it only in the viewer's own screen. */
    public static ItemStack ofViewer(ItemStack head) {
        CompoundTag mark = new CompoundTag();
        mark.putBoolean(KEY, true);
        head.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        return head;
    }

    /** Whether the item asks for a model, of the viewer or of another player. */
    public static boolean isModel(ItemStack stack) {
        return hasMark(stack, KEY) || hasMark(stack, PLAYER_KEY);
    }
}
