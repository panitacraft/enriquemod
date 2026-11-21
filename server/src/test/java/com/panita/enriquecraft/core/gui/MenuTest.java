package com.panita.enriquecraft.core.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MenuTest {

    /** A three-row menu with one button in the middle, drawn the way the caller asks. */
    private static final class SmallMenu extends Menu {
        private final boolean withFrame;
        private final boolean withFill;
        private final List<Integer> pressed = new ArrayList<>();

        SmallMenu(MenuFactory factory, boolean withFrame, boolean withFill) {
            super(factory, null);
            this.withFrame = withFrame;
            this.withFill = withFill;
        }

        @Override
        protected Component title() {
            return Component.literal("Small");
        }

        @Override
        protected int rows() {
            return 3;
        }

        @Override
        protected void draw() {
            if (withFrame) {
                frame();
            }
            set(13, MenuItem.button(factory().item(Items.DIAMOND).build(), click -> pressed.add(click.slot())));
            if (withFill) {
                fillRest();
            }
        }
    }

    @TempDir
    Path directory;

    private MenuFactory factory;

    @BeforeEach
    void createFactory() {
        factory = MinecraftTestSupport.menuFactory(directory);
    }

    @Test
    void fillRestFillsEveryEmptySlotAndKeepsWhatIsThere() {
        SmallMenu menu = new SmallMenu(factory, false, true);
        menu.prepare();

        assertEquals(Items.DIAMOND, menu.itemAt(13).stack().getItem());
        for (int slot = 0; slot < 27; slot++) {
            assertNotNull(menu.itemAt(slot), "slot " + slot);
            if (slot != 13) {
                assertEquals(factory.filler().getItem(), menu.itemAt(slot).stack().getItem(), "slot " + slot);
            }
        }
    }

    @Test
    void withoutFillRestTheOtherSlotsStayEmpty() {
        SmallMenu menu = new SmallMenu(factory, false, false);
        menu.prepare();

        assertNull(menu.itemAt(0));
        assertNull(menu.itemAt(26));
    }

    @Test
    void frameOnlyCoversTheBorder() {
        SmallMenu menu = new SmallMenu(factory, true, false);
        menu.prepare();

        assertNotNull(menu.itemAt(0));
        assertNotNull(menu.itemAt(8));
        assertNotNull(menu.itemAt(9));
        assertNotNull(menu.itemAt(26));
        assertNull(menu.itemAt(10), "the inside stays empty");
    }

    @Test
    void refreshRedrawsTheScreen() {
        SmallMenu menu = new SmallMenu(factory, false, false);
        menu.prepare();

        menu.refresh();

        assertEquals(Items.DIAMOND, menu.itemAt(13).stack().getItem());
    }

    @Test
    void theFillerIsTheSameItemEverywhere() {
        SmallMenu menu = new SmallMenu(factory, true, true);
        menu.prepare();

        assertSame(menu.itemAt(0).stack(), menu.itemAt(26).stack());
    }
}
