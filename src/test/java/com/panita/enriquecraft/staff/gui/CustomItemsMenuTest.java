package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuTesting;
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

class CustomItemsMenuTest {

    private static final Instant NOW = Instant.parse("2026-09-30T04:12:00Z");

    @TempDir
    Path directory;

    private CustomItemService service;
    private CustomItemsMenu menu;

    @BeforeEach
    void createMenu() {
        MenuFactory factory = MinecraftTestSupport.menuFactory(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), MinecraftTestSupport.ops());
        service = new CustomItemService(worldData);
        CustomItemView view = new CustomItemView(MinecraftTestSupport.messenger(directory.resolve("config")));
        menu = new CustomItemsMenu(factory, service, view);
    }

    private ItemStack shown(int slot) {
        return MenuTesting.itemAt(menu, slot).stack();
    }

    private void save(String name, ItemStack held) {
        service.save(held, name, UUID.randomUUID(), "<red>Ana", NOW);
    }

    @Test
    void eachItemIsShownAsTheRealItemInNameOrder() {
        save("zeta", new ItemStack(Items.STICK));
        save("alfa", new ItemStack(Items.DIAMOND_SWORD));

        MenuTesting.draw(menu);

        assertEquals(Items.DIAMOND_SWORD, shown(10).getItem());
        assertEquals(Items.STICK, shown(11).getItem());
    }

    @Test
    void theItemKeepsItsOwnLoreAndGetsTheDetailsAfterIt() {
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
        held.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Lore propio"))));
        save("espada", held);

        MenuTesting.draw(menu);

        List<String> lore = shown(10).get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of(
                "Lore propio",
                "",
                "ID: enriquecraft:espada",
                "Guardado por: <red>Ana",
                "Fecha: " + Timestamps.format(NOW),
                "Clic izquierdo para obtener una copia"), lore);
    }

    @Test
    void showingTheItemNeverChangesTheSavedOne() {
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
        save("espada", held);

        MenuTesting.draw(menu);

        assertEquals(List.of(), service.find("espada").orElseThrow().stack()
                .getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines(), "the menu lore must be on a copy");
        assertEquals(1, service.all().size());
    }

    @Test
    void noItemsShowsTheEmptyMarker() {
        MenuTesting.draw(menu);

        assertEquals(Items.PAPER, shown(22).getItem());
    }
}
