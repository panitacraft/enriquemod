package com.panita.enriquecraft.staff.gui;

/**
 * Where each inventory slot is shown in the six-row inspector, laid out like a player inventory:
 * the main inventory on top, the hotbar under it, then the armor from head to feet and the
 * offhand.
 */
public final class DeathInventoryLayout {

    private static final int HOTBAR_END = 9;
    private static final int MAIN_END = 36;
    private static final int FEET = 36;
    private static final int HEAD = 39;
    private static final int OFFHAND = 40;

    private static final int HOTBAR_ROW_START = 27;
    private static final int HEAD_MENU_SLOT = 36;
    private static final int OFFHAND_MENU_SLOT = 41;

    private DeathInventoryLayout() {
    }

    /**
     * @param inventorySlot 0 to 8 hotbar, 9 to 35 main inventory, 36 to 39 armor from feet to head, 40 offhand
     * @return the slot of the inspector that shows it
     * @throws IllegalArgumentException for any other slot
     */
    public static int menuSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < HOTBAR_END) {
            return HOTBAR_ROW_START + inventorySlot;
        }
        if (inventorySlot >= HOTBAR_END && inventorySlot < MAIN_END) {
            return inventorySlot - HOTBAR_END;
        }
        if (inventorySlot >= FEET && inventorySlot <= HEAD) {
            return HEAD_MENU_SLOT + (HEAD - inventorySlot);
        }
        if (inventorySlot == OFFHAND) {
            return OFFHAND_MENU_SLOT;
        }
        throw new IllegalArgumentException("Not a player inventory slot: " + inventorySlot);
    }
}
