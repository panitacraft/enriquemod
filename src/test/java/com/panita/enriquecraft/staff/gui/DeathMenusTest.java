package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeathMenusTest {

    private static final UUID ANA = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Instant WHEN = Instant.parse("2026-09-30T04:12:00Z");

    @TempDir
    Path directory;

    private UiService ui;
    private DeathInventoryService service;
    private DeathInventoryView view;

    @BeforeEach
    void create() {
        ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), MinecraftTestSupport.ops());
        StaffConfig config = new ConfigManager(directory.resolve("config")).bind("staff", StaffConfig.class);
        service = new DeathInventoryService(worldData, config);
        view = new DeathInventoryView(MinecraftTestSupport.messenger(directory.resolve("config")));
    }

    private static DeathRecord record(int minute, List<ItemStack> items) {
        List<ItemStack> all = new ArrayList<>(items);
        while (all.size() < DeathRecord.SLOT_COUNT) {
            all.add(ItemStack.EMPTY);
        }
        return new DeathRecord(UUID.randomUUID(), ANA, "Ana", WHEN.plusSeconds(minute * 60L), Level.NETHER, 10.5, 64, -20.25,
                "Ana was slain by <red>Zombie", 17, all);
    }

    private static List<ItemStack> fullSample() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            items.add(ItemStack.EMPTY);
        }
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Lore propio"))));
        items.set(0, sword);
        items.set(9, new ItemStack(Items.APPLE, 5));
        items.set(36, new ItemStack(Items.IRON_BOOTS));
        items.set(37, new ItemStack(Items.IRON_LEGGINGS));
        items.set(38, new ItemStack(Items.IRON_CHESTPLATE));
        items.set(39, new ItemStack(Items.IRON_HELMET));
        items.set(40, new ItemStack(Items.SHIELD));
        return items;
    }

    private DeathInventoryMenu inspector(DeathRecord record) {
        DeathInventoryMenu menu = new DeathInventoryMenu(ui, service, view, record, null);
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static Item itemAt(DeathInventoryMenu menu, int slot) {
        return UiTesting.itemAt(menu, slot).stack().getItem();
    }

    @Test
    void inspectorShowsEachStackWhereAPlayerInventoryWouldHaveIt() {
        DeathInventoryMenu menu = inspector(record(0, fullSample()));

        assertEquals(Items.DIAMOND_SWORD, itemAt(menu, 27), "hotbar slot 0");
        assertEquals(Items.APPLE, itemAt(menu, 0), "main inventory slot 9");
        assertEquals(Items.IRON_HELMET, itemAt(menu, 36));
        assertEquals(Items.IRON_CHESTPLATE, itemAt(menu, 37));
        assertEquals(Items.IRON_LEGGINGS, itemAt(menu, 38));
        assertEquals(Items.IRON_BOOTS, itemAt(menu, 39));
        assertEquals(Items.SHIELD, itemAt(menu, 41));
        assertEquals(5, UiTesting.itemAt(menu, 0).stack().getCount());
    }

    @Test
    void inspectorItemsKeepTheirOwnLoreAndGetTheHintAfterIt() {
        DeathInventoryMenu menu = inspector(record(0, fullSample()));

        List<String> lore = UiTesting.itemAt(menu, 27).stack().get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList();

        assertEquals(List.of("Lore propio", "", "Clic izquierdo para obtener una copia"), lore);
    }

    @Test
    void showingTheInventoryNeverChangesTheRecord() {
        DeathRecord record = record(0, fullSample());

        inspector(record);

        assertEquals(List.of("Lore propio"), record.items().get(0).get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList());
    }

    @Test
    void inspectorHasTheActionButtonsOnTheBottomRow() {
        DeathInventoryMenu menu = inspector(record(0, fullSample()));

        assertEquals(Items.OAK_DOOR, itemAt(menu, 45));
        assertEquals(Items.ENDER_PEARL, itemAt(menu, 46));
        assertEquals(Items.CHEST, itemAt(menu, 48));
        assertEquals(Items.PAPER, itemAt(menu, 49));
        assertEquals(Items.EMERALD_BLOCK, itemAt(menu, 50));
        assertEquals(Items.LAVA_BUCKET, itemAt(menu, 52));
    }

    @Test
    void everyOtherSlotOfTheInspectorIsFiller() {
        DeathInventoryMenu menu = inspector(record(0, fullSample()));

        Item filler = Items.STAINED_GLASS_PANE.black();
        for (int slot : List.of(1, 26, 28, 35, 40, 42, 44, 47, 51, 53)) {
            assertEquals(filler, itemAt(menu, slot), "slot " + slot);
        }
    }

    @Test
    void theDeleteButtonAsksForShift() {
        DeathInventoryMenu menu = inspector(record(0, fullSample()));

        List<String> lore = UiTesting.itemAt(menu, 52).stack().get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList();

        assertEquals("Mayús + clic izquierdo para confirmar.", lore.get(lore.size() - 1));
    }

    @Test
    void theRestoreButtonWarnsThatThePlayerMustBeOnline() {
        DeathInventoryMenu menu = inspector(record(0, fullSample()));

        List<String> lore = UiTesting.itemAt(menu, 50).stack().get(DataComponents.LORE).lines().stream()
                .map(Component::getString).toList();

        assertEquals("El jugador debe estar conectado.", lore.get(lore.size() - 1));
    }

    @Test
    void theListShowsTheNewestDeathFirstWithItsDetails() {
        DeathRecord older = record(0, fullSample());
        DeathRecord newer = record(30, List.of(new ItemStack(Items.APPLE, 2)));
        service.add(older);
        service.add(newer);
        DeathListMenu list = new DeathListMenu(ui, service, view, ANA, "Ana");

        UiTesting.drawAsChest(list);

        ItemStack first = UiTesting.itemAt(list, 10).stack();
        assertEquals(Items.SKELETON_SKULL, first.getItem());
        assertEquals("Muerte del " + Timestamps.format(newer.diedAt()), first.get(DataComponents.CUSTOM_NAME).getString());
        List<String> lore = first.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of(
                "Causa: Ana was slain by <red>Zombie",
                "Dimensión: Nether",
                "Posición: 10.50, 64.00, -20.25",
                "Objetos: 1",
                "Nivel de experiencia: 17",
                "Clic izquierdo para inspeccionar"), lore);
        assertEquals("Muerte del " + Timestamps.format(older.diedAt()),
                UiTesting.itemAt(list, 11).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Objetos: 7", UiTesting.itemAt(list, 11).stack().get(DataComponents.LORE).lines().get(3).getString());
    }

    @Test
    void aPlayerWithoutDeathsGetsTheEmptyMarker() {
        DeathListMenu list = new DeathListMenu(ui, service, view, ANA, "Ana");

        UiTesting.drawAsChest(list);

        assertEquals(Items.PAPER, UiTesting.itemAt(list, 22).stack().getItem());
        assertNull(UiTesting.itemAt(list, 10));
    }
}
