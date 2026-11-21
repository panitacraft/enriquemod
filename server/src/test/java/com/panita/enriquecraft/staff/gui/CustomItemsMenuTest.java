package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomItemsMenuTest {

    private static final Instant NOW = Instant.parse("2026-09-30T04:12:00Z");

    @TempDir
    Path directory;

    private CustomItemService service;
    private CustomItemsMenu menu;

    @BeforeEach
    void createMenu() {
        UiService ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), MinecraftTestSupport.ops());
        service = new CustomItemService(worldData);
        CustomItemView view = new CustomItemView(MinecraftTestSupport.messenger(directory.resolve("config")));
        menu = new CustomItemsMenu(ui, service, view);
    }

    private ItemStack shown(int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    private void save(String name, ItemStack held) {
        service.save(held, name, UUID.randomUUID(), "<red>Ana", NOW);
    }

    @Test
    void eachItemIsShownAsTheRealItemInNameOrder() {
        save("zeta", new ItemStack(Items.STICK));
        save("alfa", new ItemStack(Items.DIAMOND_SWORD));

        UiTesting.drawAsChest(menu);

        assertEquals(Items.DIAMOND_SWORD, shown(10).getItem());
        assertEquals(Items.STICK, shown(11).getItem());
    }

    @Test
    void theItemKeepsItsOwnLoreThenGetsTheIdAndWhatEachClickDoes() {
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
        held.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Lore propio"))));
        save("espada", held);

        UiTesting.drawAsChest(menu);

        List<String> lore = shown(10).get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of(
                "Lore propio",
                "",
                "enriquecraft:espada",
                "",
                "◀ Clic Izq. para obtener copia",
                "▶ Clic Der. para info"), lore);
    }

    @Test
    void theSearchBoxMatchesTheIdAndTheDisplayName() {
        ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Filo de fuego"));
        save("espada", named);
        save("palo", new ItemStack(Items.STICK));
        UiTesting.drawAsChest(menu);

        UiTesting.submit(menu, "fuego");
        assertEquals(Items.DIAMOND_SWORD, shown(10).getItem());
        assertNull(UiTesting.itemAt(menu, 11));

        UiTesting.submit(menu, "palo");
        assertEquals(Items.STICK, shown(10).getItem());
    }

    @Test
    void thereIsNoFilterDropdown() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));

        UiTesting.drawAsChest(menu);

        assertEquals(Items.STAINED_GLASS_PANE.black(), shown(47).getItem());
    }


    @Test
    void showingTheItemNeverChangesTheSavedOne() {
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
        save("espada", held);

        UiTesting.drawAsChest(menu);

        assertEquals(List.of(), service.find("espada").orElseThrow().stack()
                .getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines(), "the menu lore must be on a copy");
        assertEquals(1, service.all().size());
    }

    @Test
    void noItemsShowsTheEmptyMarker() {
        UiTesting.drawAsChest(menu);

        assertEquals(Items.PAPER, shown(22).getItem());
    }
}
