package com.panita.enriquecraft.staff.gui;

import net.minecraft.nbt.NbtOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
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
        assertEquals("→ alfa", shown(10).get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void theTooltipShowsOnlyTheDimensionAndWhatEachClickDoes() {
        service.add(coordinate("base", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        List<String> lore = shown(10).get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of(
                "Nether",
                "",
                "◀ Clic izquierdo para ir",
                "▶ Clic derecho para ver más detalles"), lore);
    }

    @Test
    void theDimensionTakesItsOwnColor() {
        service.add(coordinate("base", Items.COMPASS));

        UiTesting.drawAsChest(menu);

        Component dimension = shown(10).get(DataComponents.LORE).lines().getFirst();
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), firstColor(dimension));
    }

    private static TextColor firstColor(Component component) {
        if (component.getStyle().getColor() != null) {
            return component.getStyle().getColor();
        }
        for (Component sibling : component.getSiblings()) {
            TextColor color = firstColor(sibling);
            if (color != null) {
                return color;
            }
        }
        return null;
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
