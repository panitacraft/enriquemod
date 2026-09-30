package com.panita.enriquecraft.staff.gui;

import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuTesting;
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
        MenuFactory factory = MinecraftTestSupport.menuFactory(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), JsonOps.INSTANCE);
        service = new CoordinateService(worldData);
        CoordinateView view = new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service);
        menu = new CoordinatesMenu(factory, service, view);
    }

    private static SavedCoordinate coordinate(String name, net.minecraft.world.item.Item icon) {
        return new SavedCoordinate(name, Level.NETHER, 10.5, 64.0, -20.25, 90.0F, 15.0F, UUID.randomUUID(), "<red>Ana",
                Instant.parse("2026-09-30T04:12:00Z"), icon);
    }

    private ItemStack shown(int slot) {
        return MenuTesting.itemAt(menu, slot).stack();
    }

    @Test
    void eachCoordinateIsShownAsItsIconInNameOrder() {
        service.add(coordinate("zeta", Items.BEACON));
        service.add(coordinate("alfa", Items.COMPASS));

        MenuTesting.draw(menu);

        assertEquals(Items.COMPASS, shown(10).getItem());
        assertEquals(Items.BEACON, shown(11).getItem());
        assertEquals("alfa", shown(10).get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void theLoreShowsEveryDetail() {
        service.add(coordinate("base", Items.COMPASS));

        MenuTesting.draw(menu);

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

        MenuTesting.draw(menu);

        assertEquals("Guardada por: <red>Ana",
                shown(10).get(DataComponents.LORE).lines().get(2).getString());
    }

    @Test
    void noCoordinatesShowsTheEmptyMarker() {
        MenuTesting.draw(menu);

        assertEquals(Items.PAPER, shown(22).getItem());
    }

    @Test
    void removingACoordinateAndRefreshingUpdatesTheList() {
        service.add(coordinate("uno", Items.COMPASS));
        service.add(coordinate("dos", Items.MAP));
        MenuTesting.draw(menu);

        service.remove("uno");
        menu.refresh();

        assertEquals(Items.MAP, shown(10).getItem());
        assertNull(MenuTesting.itemAt(menu, 11));
    }
}
