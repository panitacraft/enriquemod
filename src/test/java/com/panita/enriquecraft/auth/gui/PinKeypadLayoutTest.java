package com.panita.enriquecraft.auth.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.message.PinView;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.auth.service.PinEntry;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.auth.service.PinHasher;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.gui.MenuFactory;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinKeypadLayoutTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path directory;

    private MenuFactory factory;
    private PinView view;
    private AccountService accounts;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        factory = MinecraftTestSupport.menuFactory(directory);
        AuthConfig config = new ConfigManager(directory).bind("auth", AuthConfig.class);
        view = new PinView(factory, config);
        WorldData worldData = new WorldData();
        worldData.attach(directory, NbtOps.INSTANCE);
        accounts = new AccountService(worldData, new PinHasher(new byte[32]), Instant::now);
    }

    private PinFlow entering(int length) {
        accounts.setPin(PLAYER, "Tezzlar", "1".repeat(length));
        return new PinFlow(PLAYER, "Tezzlar", new PinEntry(length, 3, true), accounts, new PinFlow.Listener() {
            @Override
            public void accepted() {
            }

            @Override
            public void wrongPin() {
            }

            @Override
            public void lockedOut() {
            }
        });
    }

    private List<ItemStack> render(PinFlow flow) {
        return PinKeypadLayout.render(factory, view.of(flow), flow);
    }

    @Test
    void theNineDigitsFillTheCraftingGridWithTheNumberAsStackSize() {
        List<ItemStack> slots = render(entering(4));

        assertEquals(PinKeypadLayout.SIZE, slots.size());
        for (int digit = 1; digit <= 9; digit++) {
            assertTrue(slots.get(digit).is(Items.STAINED_GLASS_PANE.black()), "digit " + digit);
            assertEquals(digit, slots.get(digit).getCount());
        }
    }

    @Test
    void everyKeyMapsBackFromItsSlot() {
        for (int digit = 1; digit <= 9; digit++) {
            assertEquals(digit, PinKeypadLayout.keyAt(digit));
        }
        assertEquals(PinKeypadLayout.NO_KEY, PinKeypadLayout.keyAt(32));
        assertEquals(PinKeypadLayout.BACKSPACE_KEY, PinKeypadLayout.keyAt(31));
        assertEquals(PinKeypadLayout.CLEAR_KEY, PinKeypadLayout.keyAt(33));
    }

    @Test
    void slotsThatAreNoKeyDoNothing() {
        assertEquals(PinKeypadLayout.NO_KEY, PinKeypadLayout.keyAt(0));
        assertEquals(PinKeypadLayout.NO_KEY, PinKeypadLayout.keyAt(10));
        assertEquals(PinKeypadLayout.NO_KEY, PinKeypadLayout.keyAt(19));
        assertEquals(PinKeypadLayout.NO_KEY, PinKeypadLayout.keyAt(45));
        assertEquals(PinKeypadLayout.NO_KEY, PinKeypadLayout.keyAt(-999));
    }

    @Test
    void progressShowsABarrierPerTypedDigitInTheMiddleRow() {
        PinFlow flow = entering(4);
        flow.digit(1);
        flow.digit(2);

        List<ItemStack> slots = render(flow);

        // Four digits are centered in a row of nine: columns 2 to 5 of the row that starts at slot 19.
        assertTrue(slots.get(21).is(Items.BARRIER));
        assertTrue(slots.get(22).is(Items.BARRIER));
        assertTrue(slots.get(23).is(Items.STAINED_GLASS_PANE.lightGray()));
        assertTrue(slots.get(24).is(Items.STAINED_GLASS_PANE.lightGray()));
        assertTrue(isFiller(slots.get(20)));
        assertTrue(isFiller(slots.get(25)));
    }

    @Test
    void aLongerPinUsesMoreSlots() {
        List<ItemStack> slots = render(entering(6));

        for (int column = 1; column <= 6; column++) {
            assertTrue(slots.get(19 + column).is(Items.STAINED_GLASS_PANE.lightGray()), "column " + column);
        }
        assertTrue(isFiller(slots.get(19 + 7)));
    }

    @Test
    void attemptsLeftShowInTheTopRowAndUsedOnesTurnRed() {
        PinFlow flow = entering(4);
        for (int digit = 0; digit < 4; digit++) {
            flow.digit(2);
        }

        List<ItemStack> slots = render(flow);

        assertTrue(slots.get(10 + 3).is(Items.STAINED_GLASS_PANE.lime()));
        assertTrue(slots.get(10 + 4).is(Items.STAINED_GLASS_PANE.lime()));
        assertTrue(slots.get(10 + 5).is(Items.STAINED_GLASS_PANE.red()));
    }

    @Test
    void aSuccessfulPinTurnsTheProgressGreen() {
        PinFlow flow = entering(4);
        for (int digit = 0; digit < 4; digit++) {
            flow.digit(1);
        }

        List<ItemStack> slots = render(flow);

        for (int column = 2; column <= 5; column++) {
            assertTrue(slots.get(19 + column).is(Items.STAINED_GLASS_PANE.lime()), "column " + column);
        }
    }

    @Test
    void theResultSlotIsANameTagWithTheScreensWords() {
        List<ItemStack> slots = render(entering(4));

        assertTrue(slots.get(0).is(Items.NAME_TAG));
        assertEquals("Ingresa tu PIN", slots.get(0).getHoverName().getString());
    }

    @Test
    void theRestOfTheInventoryIsBlackGlassAndTheHotbarIsEmpty() {
        List<ItemStack> slots = render(entering(4));

        for (int slot = 37; slot < PinKeypadLayout.SIZE; slot++) {
            assertTrue(slots.get(slot).isEmpty(), "hotbar slot " + slot);
        }
        // The bottom row has backspace at 31 and clear at 33; the rest of it, and of the other rows, is glass.
        for (int slot : new int[]{10, 11, 18, 19, 20, 25, 27, 28, 29, 30, 32, 34, 36}) {
            assertTrue(isFiller(slots.get(slot)), "slot " + slot);
        }
    }

    @Test
    void thereIsNoZeroKey() {
        List<ItemStack> slots = render(entering(4));

        assertTrue(slots.get(31).is(Items.ARROW));
        assertTrue(slots.get(33).is(Items.DYE.red()));
        assertTrue(isFiller(slots.get(32)));
    }

    /** Black glass that says nothing, as the game draws no tooltip for it. */
    private static boolean isFiller(ItemStack stack) {
        return stack.is(Items.STAINED_GLASS_PANE.black()) && stack.getCount() == 1
                && stack.has(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY);
    }
}
