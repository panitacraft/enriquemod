package com.panita.enriquecraft.auth.gui;

import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.auth.message.PinView;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The PIN keypad as a crafting table for players without the client companion. Every one of the 46 slots
 * is made up here, the player's own inventory included, so nothing of the real inventory is ever shown to
 * someone who has not proven who they are.
 * <p>
 * The nine digits fill the crafting grid, with the number as the stack size (a PIN has no zero); the bottom row
 * of the inventory has backspace and clear; the row above it shows how many digits are typed, and the top row
 * how many attempts are left. The rest of the inventory is black glass and the hotbar is left empty. The result
 * slot is a name tag with what the screen says.
 */
final class PinKeypadLayout {

    static final int SIZE = 46;
    /** What {@link #keyAt} says for a slot that is no key, for backspace and for clear; digits are 1 to 9. */
    static final int NO_KEY = -1;
    static final int BACKSPACE_KEY = -2;
    static final int CLEAR_KEY = -3;

    private static final int RESULT = 0;
    private static final int GRID_START = 1;
    private static final int GRID_SIZE = 9;
    private static final int INVENTORY_START = 10;
    private static final int INVENTORY_END = 37;
    private static final int ROW = 9;
    private static final int ATTEMPTS_ROW = INVENTORY_START;
    private static final int PROGRESS_ROW = INVENTORY_START + ROW;
    private static final int CONTROL_ROW = INVENTORY_START + 2 * ROW;
    private static final int BACKSPACE_SLOT = CONTROL_ROW + 3;
    private static final int CLEAR_SLOT = CONTROL_ROW + 5;

    private PinKeypadLayout() {
    }

    /** What a slot stands for: a digit from 1 to 9, {@link #BACKSPACE_KEY}, {@link #CLEAR_KEY} or {@link #NO_KEY}. */
    static int keyAt(int slot) {
        if (slot >= GRID_START && slot < GRID_START + GRID_SIZE) {
            return slot;
        }
        return switch (slot) {
            case BACKSPACE_SLOT -> BACKSPACE_KEY;
            case CLEAR_SLOT -> CLEAR_KEY;
            default -> NO_KEY;
        };
    }

    /** The contents of all 46 slots for the state of the flow. */
    static List<ItemStack> render(MenuFactory factory, PinView.Texts texts, PinFlow flow) {
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(SIZE, ItemStack.EMPTY));
        slots.set(RESULT, factory.item(Items.NAME_TAG).name(texts.title()).loreLines(List.of(texts.hint())).build());
        for (int digit = 1; digit <= GRID_SIZE; digit++) {
            slots.set(GRID_START + digit - 1, digit(factory, digit));
        }
        slots.set(BACKSPACE_SLOT, factory.item(Items.ARROW).name(AuthMessages.Pin.BACKSPACE)
                .lore(AuthMessages.Pin.BACKSPACE_LORE).build());
        slots.set(CLEAR_SLOT, factory.item(Items.DYE.red()).name(AuthMessages.Pin.CLEAR)
                .lore(AuthMessages.Pin.CLEAR_LORE).build());
        placeProgress(factory, flow, slots);
        placeAttempts(factory, flow, slots);
        // Whatever is left of the inventory, but not the hotbar, is black glass with no name.
        ItemStack filler = factory.item(Items.STAINED_GLASS_PANE.black()).hideTooltip().build();
        for (int slot = INVENTORY_START; slot < INVENTORY_END; slot++) {
            if (slots.get(slot).isEmpty()) {
                slots.set(slot, filler.copy());
            }
        }
        return slots;
    }

    /** A digit key: the number is the stack size, which the client draws over the item. */
    private static ItemStack digit(MenuFactory factory, int digit) {
        return factory.item(Items.STAINED_GLASS_PANE.black()).amount(digit)
                .name(Message.plain(AuthMessages.Pin.DIGIT).with("digit", digit))
                .lore(AuthMessages.Pin.DIGIT_LORE).build();
    }

    private static void placeProgress(MenuFactory factory, PinFlow flow, List<ItemStack> slots) {
        int first = PROGRESS_ROW + (ROW - flow.shownLength()) / 2;
        for (int index = 0; index < flow.shownLength(); index++) {
            boolean typed = index < flow.shownDigits();
            Item item;
            String name;
            if (!typed) {
                item = Items.STAINED_GLASS_PANE.lightGray();
                name = AuthMessages.Pin.PENDING;
            } else if (flow.success()) {
                item = Items.STAINED_GLASS_PANE.lime();
                name = AuthMessages.Pin.TYPED;
            } else if (flow.phase() == PinFlow.Phase.WRONG) {
                item = Items.STAINED_GLASS_PANE.red();
                name = AuthMessages.Pin.TYPED;
            } else {
                item = Items.BARRIER;
                name = AuthMessages.Pin.TYPED;
            }
            slots.set(first + index, factory.item(item).name(name).build());
        }
    }

    private static void placeAttempts(MenuFactory factory, PinFlow flow, List<ItemStack> slots) {
        int total = flow.maxAttempts();
        int first = ATTEMPTS_ROW + (ROW - total) / 2;
        for (int index = 0; index < total; index++) {
            boolean left = index < flow.attemptsLeft();
            slots.set(first + index, factory.item(left ? Items.STAINED_GLASS_PANE.lime() : Items.STAINED_GLASS_PANE.red())
                    .name(left ? AuthMessages.Pin.ATTEMPT_LEFT : AuthMessages.Pin.ATTEMPT_USED).build());
        }
    }
}
