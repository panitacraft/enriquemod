package com.panita.enriquecraft.staff.gui;

import net.minecraft.nbt.NbtOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.core.component.DataComponents;
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
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void eachCoordinateIsShownAsItsIconInNameOrder() {
        service.add(coordinate("zeta", Items.BEACON));
        service.add(coordinate("alfa", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        assertEquals(Items.COMPASS, shown(10).getItem());
        assertEquals(Items.BEACON, shown(11).getItem());
        assertEquals("alfa", shown(10).get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void theLoreShowsEveryDetail() {
        service.add(coordinate("base", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        List<String> lore = shown(10).get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of(
                "Dimensión: Nether",
                "Posición: 10.50, 64.00, -20.25",
                "Guardada por: <red>Ana",
                "Fecha: " + com.panita.enriquecraft.core.message.Timestamps.format(Instant.parse("2026-09-30T04:12:00Z")),
                "Clic izquierdo para teletransportarte"), lore);
    }

    @Test
    void namesAndPlayersAreShownLiterallyNotAsTags() {
        service.add(coordinate("base", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        assertEquals("Guardada por: <red>Ana",
                shown(10).get(DataComponents.LORE).lines().get(2).getString());
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
}
