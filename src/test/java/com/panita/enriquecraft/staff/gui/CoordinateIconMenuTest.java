package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.PlayerRef;
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
import net.minecraft.world.item.component.ResolvableProfile;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateIconMenuTest {

    /** The first two entries are the doors to the head and the map choices, so the icons start after them. */
    private static final int DOORS = 2;
    private static final int FIRST_CELL = 10;
    private static final PlayerRef NOTCH = new PlayerRef("Notch", UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"));

    @TempDir
    Path directory;

    private CoordinateService service;
    private CoordinateIconMenu menu;
    private UiService ui;
    private Item current;

    @BeforeEach
    void create() {
        ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        // Items exist only once Minecraft is started, which the line above does.
        current = CoordinateIcons.all().get(3);
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
        service.add(new SavedCoordinate("base", Level.OVERWORLD, 0, 64, 0, 0, 0, UUID.randomUUID(), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), current));
        draw();
    }

    private void draw() {
        CoordinateView view = new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service);
        menu = new CoordinateIconMenu(ui, service, "base", new CoordinatesMenu(ui, service, view));
        UiTesting.drawAsChest(menu);
    }

    /** The chest slot of the nth entry of the list: seven to a row, rows nine slots apart. */
    private static int slotOfEntry(int entry) {
        return FIRST_CELL + (entry / 7) * 9 + entry % 7;
    }

    private static int slotOfIcon(int icon) {
        return slotOfEntry(icon + DOORS);
    }

    private ItemStack shown(int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void theHeadAndMapChoicesComeFirstThenEveryIconInOrder() {
        List<Item> icons = CoordinateIcons.all();

        assertEquals(Items.PLAYER_HEAD, shown(slotOfEntry(0)).getItem());
        assertEquals(Items.ABANDONED_CAMP_MAP, shown(slotOfEntry(1)).getItem());
        assertEquals(icons.get(0), shown(slotOfIcon(0)).getItem());
        assertEquals(icons.get(5), shown(slotOfIcon(5)).getItem(), "the grid wraps after seven");
        assertEquals(icons.get(25), shown(slotOfIcon(25)).getItem(), "twenty eight to a page");
    }

    @Test
    void theCurrentIconShinesAndTheOthersDoNot() {
        assertEquals(true, shown(slotOfIcon(3)).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertNull(shown(slotOfIcon(0)).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Test
    void eachIconSaysWhatClickingItDoes() {
        assertEquals("◀ Clic Izq. para elegir", lore(shown(slotOfIcon(0))).getFirst());
        assertEquals("Icono actual", lore(shown(slotOfIcon(3))).getFirst());
    }

    @Test
    void anIconKeepsItsOwnName() {
        assertNull(shown(slotOfIcon(0)).get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void thereIsABackButton() {
        assertEquals(Items.OAK_DOOR, shown(45).getItem());
    }

    @Test
    void theLoreIsTheOnlyTextAddedToAnIcon() {
        assertEquals(1, lore(shown(slotOfIcon(0))).size());
    }

    @Test
    void theHeadChoiceExplainsItselfAndLeadsToAskingForAName() {
        ItemStack head = shown(slotOfEntry(0));

        assertEquals("Cabeza de jugador", head.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(List.of("Usa la cabeza de un jugador como icono", "", "◀ Clic Izq. para elegir jugador"), lore(head));
    }

    @Test
    void aCoordinateShownAsAHeadShinesOnTheHeadChoiceAndNamesThePlayer() {
        service.updateIconHead("base", NOTCH);
        draw();

        ItemStack head = shown(slotOfEntry(0));

        assertEquals(true, head.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertEquals("Actual: Notch", lore(head).get(1));
        assertNull(shown(slotOfIcon(3)).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "no plain icon is current any more");
    }

    @Test
    void theMapChoiceLeadsToTheStructureMaps() {
        ItemStack maps = shown(slotOfEntry(1));

        assertEquals("Mapas de estructuras", maps.get(DataComponents.CUSTOM_NAME).getString());
        assertNull(maps.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Test
    void aCoordinateShownAsAStructureMapShinesOnTheMapChoice() {
        service.updateIcon("base", Items.ABANDONED_CAMP_MAP);
        draw();

        assertEquals(true, shown(slotOfEntry(1)).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Test
    void headsKeepTheirProfileSoTheViewerCanShowTheSkin() {
        service.updateIconHead("base", NOTCH);

        ResolvableProfile profile = service.find("base").orElseThrow().iconStack().get(DataComponents.PROFILE);

        assertTrue(profile != null);
    }
}
