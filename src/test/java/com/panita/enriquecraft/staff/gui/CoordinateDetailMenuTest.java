package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateDetailMenuTest {

    private static final Instant WHEN = Instant.parse("2026-09-30T04:12:00Z");

    @TempDir
    Path directory;

    private UiService ui;
    private CoordinateService service;
    private CoordinateView view;

    @BeforeEach
    void create() {
        ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
        view = new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service);
    }

    private void save(String name, Item icon) {
        service.add(new SavedCoordinate(name, Level.NETHER, 10.5, 64.0, -20.25, 90.0F, 15.0F, UUID.randomUUID(), "<red>Ana",
                WHEN, icon));
    }

    private CoordinateDetailMenu detail(String name) {
        CoordinateDetailMenu menu = new CoordinateDetailMenu(ui, service, view, name, new CoordinatesMenu(ui, service, view));
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static ItemStack stack(CoordinateDetailMenu menu, int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void theCoordinateIsShownAsItsIconUnderItsDisplayName() {
        save("base", Items.DIAMOND);
        service.updateDisplayName("base", "Mi base");

        CoordinateDetailMenu menu = detail("base");

        ItemStack shown = stack(menu, 4);
        assertEquals(Items.DIAMOND, shown.getItem());
        assertEquals("Mi base", shown.get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void theLinesKeepTheIdSeparateAndShowReadableData() {
        save("base", Items.DIAMOND);

        List<String> lore = lore(stack(detail("base"), 4));

        assertEquals("ID: base", lore.get(0));
        assertEquals("Dimensión: Nether", lore.get(1));
        assertEquals("Posición: 10, 64, -21", lore.get(2), "only whole blocks are shown, rounding down");
        assertTrue(lore.get(3).startsWith("Guardada por: ") && lore.get(3).endsWith("<red>Ana"), lore.get(3));
        assertEquals("Fecha: " + Timestamps.date(WHEN), lore.get(4));
    }

    @Test
    void pressingTheIconIsAdvertisedAtTheEndOfTheLore() {
        save("base", Items.DIAMOND);

        List<String> lore = lore(stack(detail("base"), 4));

        assertEquals("", lore.get(5));
        assertEquals("◀ Clic Izq. para cambiar icono", lore.get(6));
    }

    @Test
    void theNameCanBeEditedInAFieldBelowTheIcon() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        assertEquals(Items.NAME_TAG, stack(menu, 13).getItem());
        assertEquals("Nombre visible", stack(menu, 13).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Actual: base", lore(stack(menu, 13)).getFirst());
    }

    @Test
    void submittingANameChangesTheDisplayNameAndKeepsTheId() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        UiTesting.submit(menu, "Mi base");

        assertEquals("Mi base", service.find("base").orElseThrow().displayName());
        assertEquals("base", service.find("base").orElseThrow().name());
        assertEquals("Mi base", stack(menu, 4).get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void aBlankNameIsIgnored() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        UiTesting.submit(menu, "   ");

        assertEquals("base", service.find("base").orElseThrow().displayName());
    }

    @Test
    void theControlsAreBackTeleportAndDelete() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        assertEquals(Items.OAK_DOOR, stack(menu, 18).getItem());
        assertEquals(Items.ENDER_PEARL, stack(menu, 21).getItem());
        assertEquals(Items.LAVA_BUCKET, stack(menu, 22).getItem());
        assertEquals("Eliminar", stack(menu, 22).get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void thereIsNoSeparateChangeIconButtonSinceTheIconIsThePress() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        for (int slot = 18; slot < 27; slot++) {
            assertTrue(!"Cambiar icono".equals(stack(menu, slot).has(DataComponents.CUSTOM_NAME)
                    ? stack(menu, slot).get(DataComponents.CUSTOM_NAME).getString() : ""), "slot " + slot);
        }
    }

    @Test
    void everyOtherSlotIsFiller() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        Item filler = Items.STAINED_GLASS_PANE.black();
        for (int slot : List.of(0, 3, 5, 8, 9, 12, 14, 17, 19, 20, 23, 26)) {
            assertEquals(filler, stack(menu, slot).getItem(), "slot " + slot);
        }
    }

    @Test
    void theClientCompanionSeesAPressableDetailAndTheRolesOfItsControls() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));

        UiElement.Detail detail = assertInstanceOf(UiElement.Detail.class, root.children().getFirst());
        assertTrue(detail.iconId() != UiElement.Detail.NOT_PRESSABLE);
        assertInstanceOf(UiElement.TextInput.class, root.children().get(1));
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());
        assertEquals(ButtonRole.BACK, assertInstanceOf(UiElement.Button.class, controls.children().getFirst()).role());
    }

    @Test
    void aNewIconShowsOnTheNextRedraw() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        service.updateIcon("base", Items.EMERALD);
        menu.refresh();

        assertEquals(Items.EMERALD, stack(menu, 4).getItem());
    }

    @Test
    void aCoordinateRemovedWhileShownLeavesOnlyAMessageAndTheBackButton() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        service.remove("base");
        menu.refresh();

        assertEquals(Items.BARRIER, stack(menu, 4).getItem());
        assertEquals(Items.OAK_DOOR, stack(menu, 18).getItem());
    }
}
