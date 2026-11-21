package com.panita.enriquecraft.staff.gui;

import net.minecraft.nbt.NbtOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinatesMenuTest {

    @TempDir
    Path directory;

    private CoordinateService service;
    private CoordinatesMenu menu;

    @BeforeEach
    void createMenu() {
        UiService ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
        CoordinateView view = new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service);
        menu = new CoordinatesMenu(ui, service, view);
    }

    private static SavedCoordinate coordinate(String name, net.minecraft.world.item.Item icon) {
        return new SavedCoordinate(name, Level.NETHER, 10.5, 64.0, -20.25, 90.0F, 15.0F, UUID.randomUUID(), "<red>Ana",
                Instant.parse("2026-09-30T04:12:00Z"), icon);
    }

    private ItemStack shown(int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    @Test
    void eachCoordinateIsShownAsItsIconInTheOrderStaffArrangedThem() {
        service.add(coordinate("zeta", Items.BEACON));
        service.add(coordinate("alfa", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        assertEquals(Items.BEACON, shown(10).getItem(), "a new coordinate goes last, not into alphabetical order");
        assertEquals(Items.COMPASS, shown(11).getItem());
        assertEquals("→ zeta", shown(10).get(DataComponents.CUSTOM_NAME).getString());
    }

    private void addThree() {
        service.add(coordinate("a", Items.BEACON));
        service.add(coordinate("b", Items.COMPASS));
        service.add(coordinate("c", Items.CLOCK));
    }

    private List<String> order() {
        return service.all().stream().map(SavedCoordinate::name).toList();
    }

    @Test
    void aChestHasAnArrangeSwitchInItsControls() {
        addThree();

        UiTesting.drawAsChest(menu);

        assertEquals(Items.HOPPER, shown(52).getItem());
        assertEquals("Reordenar", shown(52).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Desactivado", lore(shown(52)).getFirst());
    }

    @Test
    void whileArrangingAClickLiftsACoordinateAndAnotherClickPlacesItInThatSpot() {
        addThree();
        UiTesting.drawAsChest(menu);

        UiTesting.click(menu, 52);
        UiTesting.click(menu, 10);
        UiTesting.click(menu, 12);

        assertEquals(List.of("b", "c", "a"), order());
    }

    @Test
    void movingBackwardsPutsItBeforeTheOneItLandsOn() {
        addThree();
        UiTesting.drawAsChest(menu);

        UiTesting.click(menu, 52);
        UiTesting.click(menu, 12);
        UiTesting.click(menu, 10);

        assertEquals(List.of("c", "a", "b"), order());
    }

    @Test
    void whileArrangingTheLiftedOneIsMarkedAndTheHintsFollow() {
        addThree();
        UiTesting.drawAsChest(menu);
        UiTesting.click(menu, 52);

        assertEquals("◀ Clic Izq. para levantar", lore(shown(10)).getLast());
        assertEquals("Activado", lore(shown(52)).getFirst());

        UiTesting.click(menu, 10);

        assertEquals("↕ → a", shown(10).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("◀ Clic Izq. para soltar", lore(shown(10)).getLast());
        assertEquals("◀ Clic Izq. para colocar aquí", lore(shown(11)).getLast());
    }

    @Test
    void clickingTheLiftedOneAgainLetsItGoWithoutMovingAnything() {
        addThree();
        UiTesting.drawAsChest(menu);

        UiTesting.click(menu, 52);
        UiTesting.click(menu, 10);
        UiTesting.click(menu, 10);

        assertEquals(List.of("a", "b", "c"), order());
        assertEquals("◀ Clic Izq. para levantar", lore(shown(11)).getLast());
    }

    @Test
    void theClientCompanionDragsInsteadAndHasNoArrangeSwitch() {
        addThree();
        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));
        UiElement.Grid grid = assertInstanceOf(UiElement.Grid.class, root.children().getFirst());
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());

        assertTrue(grid.children().stream().allMatch(child -> ((UiElement.Button) child).draggable()));
        assertTrue(controls.children().stream().noneMatch(child ->
                child instanceof UiElement.Button button && button.icon().is(Items.HOPPER)));
    }

    @Test
    void droppingOneCoordinateOnAnotherTakesItsPlaceForEveryone() {
        addThree();

        UiTesting.dragOnto(menu, 0, 2);

        assertEquals(List.of("b", "c", "a"), order());
        UiTesting.dragOnto(menu, 2, 0);
        assertEquals(List.of("a", "b", "c"), order());
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void theTooltipShowsOnlyTheDimensionAndWhatEachClickDoes() {
        service.add(coordinate("base", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        List<String> lore = shown(10).get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of(
                "Dimensión: Nether",
                "",
                "◀ Clic Izq. para ir",
                "▶ Clic Der. para info"), lore);
    }

    @Test
    void theDimensionTakesItsOwnColor() {
        service.add(coordinate("base", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        Component dimension = shown(10).get(DataComponents.LORE).lines().getFirst();
        assertTrue(usesColor(dimension, TextColor.fromLegacyFormat(ChatFormatting.RED)));
    }

    private static boolean usesColor(Component component, TextColor color) {
        if (color.equals(component.getStyle().getColor())) {
            return true;
        }
        return component.getSiblings().stream().anyMatch(sibling -> usesColor(sibling, color));
    }


    @Test
    void noCoordinatesShowsTheEmptyMarker() {
        UiTesting.drawAsChest(menu);

        assertEquals(Items.PAPER, shown(22).getItem());
    }

    @Test
    void removingACoordinateAndRefreshingUpdatesTheList() {
        service.add(coordinate("uno", Items.COMPASS));
        service.add(coordinate("dos", Items.MAP));
        UiTesting.drawAsChest(menu);

        service.remove("uno");
        menu.refresh();

        assertEquals(Items.MAP, shown(10).getItem());
        assertNull(UiTesting.itemAt(menu, 11));
    }

    @Test
    void searchingByNameKeepsOnlyTheMatchingCoordinates() {
        service.add(coordinate("Base_norte", Items.COMPASS));
        service.add(coordinate("mina", Items.MAP));
        service.add(coordinate("base_sur", Items.BEACON));
        UiTesting.drawAsChest(menu);

        UiTesting.submit(menu, "BASE");

        assertEquals("→ Base_norte", shown(10).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("→ base_sur", shown(11).get(DataComponents.CUSTOM_NAME).getString());
        assertNull(UiTesting.itemAt(menu, 12));
    }
}
