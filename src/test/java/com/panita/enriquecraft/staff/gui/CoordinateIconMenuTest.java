package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateIcons;
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
import static org.junit.jupiter.api.Assertions.assertNull;

class CoordinateIconMenuTest {

    @TempDir
    Path directory;

    private CoordinateService service;
    private CoordinateIconMenu menu;
    private final Item current = CoordinateIcons.all().get(3);

    @BeforeEach
    void create() {
        UiService ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
        service.add(new SavedCoordinate("base", Level.OVERWORLD, 0, 64, 0, 0, 0, UUID.randomUUID(), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), current));
        CoordinateView view = new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service);
        menu = new CoordinateIconMenu(ui, service, "base", new CoordinatesMenu(ui, service, view));
        UiTesting.drawAsChest(menu);
    }

    private ItemStack shown(int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    @Test
    void everyAvailableIconIsOfferedInOrder() {
        List<Item> icons = CoordinateIcons.all();

        assertEquals(icons.get(0), shown(10).getItem());
        assertEquals(icons.get(1), shown(11).getItem());
        assertEquals(icons.get(7), shown(19).getItem(), "the grid wraps after seven");
        assertEquals(icons.get(27), shown(43).getItem(), "twenty eight to a page");
    }

    @Test
    void theCurrentIconShinesAndTheOthersDoNot() {
        assertEquals(true, shown(13).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertNull(shown(10).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Test
    void eachIconSaysWhatClickingItDoes() {
        assertEquals("◀ Clic Izq. para elegir",
                shown(10).get(DataComponents.LORE).lines().getFirst().getString());
        assertEquals("Icono actual", shown(13).get(DataComponents.LORE).lines().getFirst().getString());
    }

    @Test
    void anIconKeepsItsOwnName() {
        assertNull(shown(10).get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void thereIsABackButton() {
        assertEquals(Items.OAK_DOOR, shown(45).getItem());
    }

    @Test
    void theLoreIsTheOnlyTextAddedToAnIcon() {
        List<String> lore = shown(10).get(DataComponents.LORE).lines().stream().map(Component::getString).toList();

        assertEquals(1, lore.size());
    }
}
