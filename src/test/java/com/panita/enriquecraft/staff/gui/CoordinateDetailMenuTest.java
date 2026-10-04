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

    private CoordinatesMenu list() {
        return new CoordinatesMenu(ui, service, view);
    }

    private CoordinateDetailMenu detail(String name) {
        CoordinateDetailMenu menu = new CoordinateDetailMenu(ui, service, view, name, list());
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static ItemStack stack(CoordinateDetailMenu menu, int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    @Test
    void theCoordinateIsShownLargeAsItsIconWithEveryDetail() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        ItemStack shown = stack(menu, 4);
        assertEquals(Items.DIAMOND, shown.getItem());
        assertEquals("base", shown.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(List.of(
                "Dimensión: Nether",
                "Posición: 10.50, 64.00, -20.25",
                "Guardada por: <red>Ana",
                "Fecha: " + Timestamps.date(WHEN)),
                shown.get(DataComponents.LORE).lines().stream().map(Component::getString).toList());
    }

    @Test
    void theControlsAreBackChangeIconAndTeleport() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        assertEquals(Items.OAK_DOOR, stack(menu, 9).getItem());
        assertEquals(Items.DIAMOND, stack(menu, 12).getItem(), "the change icon button shows the current icon");
        assertEquals("Cambiar icono", stack(menu, 12).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(Items.ENDER_PEARL, stack(menu, 14).getItem());
    }

    @Test
    void everyOtherSlotIsFiller() {
        save("base", Items.DIAMOND);

        CoordinateDetailMenu menu = detail("base");

        Item filler = Items.STAINED_GLASS_PANE.black();
        for (int slot : List.of(0, 3, 5, 8, 10, 11, 13, 15, 17)) {
            assertEquals(filler, stack(menu, slot).getItem(), "slot " + slot);
        }
    }

    @Test
    void theClientCompanionSeesTheDetailAndTheRolesOfItsControls() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));

        assertInstanceOf(UiElement.Detail.class, root.children().getFirst());
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
        assertEquals(Items.EMERALD, stack(menu, 12).getItem());
    }

    @Test
    void aCoordinateRemovedWhileShownLeavesOnlyAMessageAndTheBackButton() {
        save("base", Items.DIAMOND);
        CoordinateDetailMenu menu = detail("base");

        service.remove("base");
        menu.refresh();

        assertEquals(Items.BARRIER, stack(menu, 4).getItem());
        assertEquals(Items.OAK_DOOR, stack(menu, 9).getItem());
        assertEquals(Items.STAINED_GLASS_PANE.black(), stack(menu, 12).getItem());
    }
}
