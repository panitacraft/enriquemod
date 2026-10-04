package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChestLayoutTest {

    /** A menu whose screen is whatever the test builds. */
    private static final class ScreenMenu extends UiMenu {
        private final ChestStyle style;
        private final Function<UiBuilder, UiElement> screen;

        ScreenMenu(UiService ui, ChestStyle style, Function<UiBuilder, UiElement> screen) {
            super(ui, null);
            this.style = style;
            this.screen = screen;
        }

        @Override
        protected Component title() {
            return Component.literal("Screen");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            return screen.apply(builder);
        }

        @Override
        protected ChestStyle chestStyle() {
            return style;
        }
    }

    @TempDir
    Path directory;

    private UiService ui;

    @BeforeEach
    void createService() {
        ui = MinecraftTestSupport.uiService(directory);
    }

    private ScreenMenu screen(ChestStyle style, Function<UiBuilder, UiElement> screen) {
        return new ScreenMenu(ui, style, screen);
    }

    private static UiElement.Button button(UiBuilder builder, String name, List<String> pressed) {
        return builder.button(new ItemStack(Items.STONE), Component.literal(name), List.of(), click -> pressed.add(name));
    }

    @Test
    void aRowFillsOneChestRowFromTheLeft() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Row(List.of(button(b, "a", pressed), button(b, "b", pressed))));

        UiTesting.drawAsChest(menu);

        assertEquals(1, UiTesting.rows(menu));
        assertEquals("a", UiTesting.itemAt(menu, 0).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("b", UiTesting.itemAt(menu, 1).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertNull(UiTesting.itemAt(menu, 2));
    }

    @Test
    void aSpacerLeavesItsCellEmpty() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Row(
                List.of(new UiElement.Spacer(), button(b, "a", pressed))));

        UiTesting.drawAsChest(menu);

        assertNull(UiTesting.itemAt(menu, 0));
        assertNotNull(UiTesting.itemAt(menu, 1));
    }

    @Test
    void aSingleElementSitsInTheMiddleOfItsRow() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> button(b, "a", pressed));

        UiTesting.drawAsChest(menu);

        assertNotNull(UiTesting.itemAt(menu, 4));
    }

    @Test
    void aGridTakesItsDeclaredRowsAndWrapsAtItsColumns() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Grid(3, 2, List.of(
                button(b, "a", pressed), button(b, "b", pressed), button(b, "c", pressed), button(b, "d", pressed))));

        UiTesting.drawAsChest(menu);

        assertEquals(2, UiTesting.rows(menu));
        assertNotNull(UiTesting.itemAt(menu, 2));
        assertEquals("d", UiTesting.itemAt(menu, 9).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertNull(UiTesting.itemAt(menu, 3));
    }

    @Test
    void bandsOfAColumnStackFromTheTop() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Column(List.of(
                new UiElement.Row(List.of(button(b, "top", pressed))),
                new UiElement.Row(List.of(button(b, "bottom", pressed))))));

        UiTesting.drawAsChest(menu);

        assertEquals(2, UiTesting.rows(menu));
        assertEquals("top", UiTesting.itemAt(menu, 0).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("bottom", UiTesting.itemAt(menu, 9).stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void aFramedScreenKeepsItsEdgeAndPutsTheLastBandOnTheBottomEdge() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.FRAMED, b -> new UiElement.Column(List.of(
                new UiElement.Row(List.of(button(b, "inside", pressed))),
                new UiElement.Row(List.of(new UiElement.Spacer(), button(b, "control", pressed))))));

        UiTesting.drawAsChest(menu);

        assertEquals(3, UiTesting.rows(menu));
        assertEquals(Items.STAINED_GLASS_PANE.black(), UiTesting.itemAt(menu, 0).stack().getItem());
        assertEquals("inside", UiTesting.itemAt(menu, 10).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("control", UiTesting.itemAt(menu, 19).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(Items.STAINED_GLASS_PANE.black(), UiTesting.itemAt(menu, 18).stack().getItem(),
                "a spacer on the bottom edge leaves the frame");
    }

    @Test
    void aLabelIsShownAsANamedItem() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Label(Component.literal("Hola")));

        UiTesting.drawAsChest(menu);

        assertEquals("Hola", UiTesting.itemAt(menu, 4).stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void aButtonWithoutAnIconIsShownAsPaper() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> b.button(ItemStack.EMPTY, Component.literal("x"), List.of(), click -> { }));

        UiTesting.drawAsChest(menu);

        assertEquals(Items.PAPER, UiTesting.itemAt(menu, 4).stack().getItem());
    }

    @Test
    void theTooltipBecomesTheLore() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> b.button(new ItemStack(Items.STONE), Component.literal("x"),
                List.of(Component.literal("uno"), Component.literal("dos")), click -> { }));

        UiTesting.drawAsChest(menu);

        List<String> lore = UiTesting.itemAt(menu, 4).stack().get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList();
        assertEquals(List.of("uno", "dos"), lore);
    }

    @Test
    void aRowWiderThanTheChestIsRejected() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Row(Collections.nCopies(10, new UiElement.Spacer())));

        assertThrows(IllegalStateException.class, () -> UiTesting.drawAsChest(menu));
    }

    @Test
    void aFramedRowWiderThanTheInteriorIsRejected() {
        ScreenMenu menu = screen(ChestStyle.FRAMED, b -> new UiElement.Column(List.of(
                new UiElement.Row(Collections.nCopies(8, new UiElement.Spacer())),
                new UiElement.Row(List.of()))));

        assertThrows(IllegalStateException.class, () -> UiTesting.drawAsChest(menu));
    }

    @Test
    void moreThanSixRowsIsRejected() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Grid(1, 7, List.of()));

        assertThrows(IllegalStateException.class, () -> UiTesting.drawAsChest(menu));
    }

    @Test
    void anEmptyScreenIsRejected() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Column(List.of()));

        assertThrows(IllegalStateException.class, () -> UiTesting.drawAsChest(menu));
    }

    @Test
    void aContainerInsideARowIsRejected() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Row(List.of(new UiElement.Row(List.of()))));

        assertThrows(IllegalStateException.class, () -> UiTesting.drawAsChest(menu));
    }

    @Test
    void pressingAButtonRunsItsAction() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Row(List.of(button(b, "a", pressed), button(b, "b", pressed))));
        UiTesting.drawAsChest(menu);

        UiTesting.click(menu, 1);
        UiTesting.click(menu, 0);

        assertEquals(List.of("b", "a"), pressed);
    }

    @Test
    void aFilledScreenFillsEverySlotWithoutAnElement() {
        List<String> pressed = new ArrayList<>();
        ScreenMenu menu = screen(ChestStyle.FILLED, b -> new UiElement.Row(
                List.of(button(b, "a", pressed), new UiElement.Spacer())));

        UiTesting.drawAsChest(menu);

        assertEquals(Items.STONE, UiTesting.itemAt(menu, 0).stack().getItem());
        for (int slot = 1; slot < 9; slot++) {
            assertEquals(Items.STAINED_GLASS_PANE.black(), UiTesting.itemAt(menu, slot).stack().getItem(), "slot " + slot);
        }
    }

    @Test
    void aPlainScreenLeavesItsEmptySlotsEmpty() {
        ScreenMenu menu = screen(ChestStyle.PLAIN, b -> new UiElement.Row(List.of(new UiElement.Spacer())));

        UiTesting.drawAsChest(menu);

        assertNull(UiTesting.itemAt(menu, 0));
    }
}
