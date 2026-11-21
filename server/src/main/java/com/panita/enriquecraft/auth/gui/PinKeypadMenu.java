package com.panita.enriquecraft.auth.gui;

import com.panita.enriquecraft.core.ui.PinKeys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The crafting table behind the PIN keypad of players without the client companion. It is display only:
 * no click ever moves an item, and a left click on a key is the only thing that does anything. The vanilla
 * crafting screen has a recipe book button that cannot be removed from here; the server ignores whatever
 * it sends, since this is not a menu that crafts.
 * <p>
 * Nothing here calls {@code super.clicked}: that single omission is what makes the screen safe.
 */
final class PinKeypadMenu extends AbstractContainerMenu {

    private final SimpleContainer container = new SimpleContainer(PinKeypadLayout.SIZE);
    private final PinKeys keys;
    private final Runnable onRemoved;

    PinKeypadMenu(int containerId, PinKeys keys, Runnable onRemoved) {
        super(MenuType.CRAFTING, containerId);
        this.keys = keys;
        this.onRemoved = onRemoved;
        for (int slot = 0; slot < PinKeypadLayout.SIZE; slot++) {
            addSlot(new Slot(container, slot, 0, 0) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    /** Replaces what the 46 slots show; the player sees it on the next tick. */
    void show(List<ItemStack> stacks) {
        for (int slot = 0; slot < PinKeypadLayout.SIZE; slot++) {
            container.setItem(slot, stacks.get(slot));
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (input == ContainerInput.PICKUP && button == 0) {
                press(PinKeypadLayout.keyAt(slotIndex));
            }
            // The client predicted a pickup that did not happen, and the keys may have changed what is shown.
            if (serverPlayer.containerMenu == this) {
                sendAllDataToRemote();
            }
        }
    }

    private void press(int key) {
        switch (key) {
            case PinKeypadLayout.NO_KEY -> { }
            case PinKeypadLayout.BACKSPACE_KEY -> keys.backspace();
            case PinKeypadLayout.CLEAR_KEY -> keys.clear();
            default -> keys.digit(key);
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
        onRemoved.run();
    }
}
