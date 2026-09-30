package com.panita.enriquecraft.core.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The vanilla chest screen behind a {@link Menu}. It is display only: no click ever moves,
 * drags, swaps, drops or clones an item, whatever the click type. Every click that lands on a
 * menu slot is handed to the menu, and the client is then resynced so it never keeps a
 * prediction of a change that did not happen.
 * <p>
 * Nothing here calls {@code super.clicked}: that single omission is what makes the screen safe.
 */
final class MenuScreen extends ChestMenu {

    private static final List<MenuType<ChestMenu>> TYPES = List.of(
            MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6);

    private final Menu owner;
    private final int menuSlots;

    MenuScreen(int containerId, Inventory playerInventory, SimpleContainer container, int rows, Menu owner) {
        super(TYPES.get(rows - 1), containerId, playerInventory, container, rows);
        this.owner = owner;
        this.menuSlots = container.getContainerSize();
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (slotIndex >= 0 && slotIndex < menuSlots) {
                owner.handleClick(new MenuClick(serverPlayer, slotIndex, button, input));
            }
            // The click may have opened another screen; only resync while this one is still open.
            if (serverPlayer.containerMenu == this) {
                sendAllDataToRemote();
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        owner.closed();
    }
}
