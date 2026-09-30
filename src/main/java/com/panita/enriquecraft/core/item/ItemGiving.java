package com.panita.enriquecraft.core.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

/**
 * Hands items to players without ever losing them.
 */
public final class ItemGiving {

    private ItemGiving() {
    }

    /**
     * Gives a player a copy of a stack. It joins stacks the player already has, then takes free
     * slots, and whatever still does not fit drops at the player's feet. The given stack is not
     * changed.
     */
    public static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
    }
}
