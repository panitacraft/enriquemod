package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathPlayersMenuTest {

    private static final UUID ANA = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID BEA = UUID.fromString("66666666-7777-8888-9999-000000000000");
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
        service = new DeathInventoryService(worldData,
                new ConfigManager(directory.resolve("config")).bind("staff", StaffConfig.class));
        view = new DeathInventoryView(MinecraftTestSupport.messenger(directory.resolve("config")));
    }

    private void died(UUID player, String name, int minute) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            items.add(slot == 0 ? new ItemStack(Items.APPLE) : ItemStack.EMPTY);
        }
        service.add(new DeathRecord(UUID.randomUUID(), player, name, WHEN.plusSeconds(minute * 60L), Level.OVERWORLD,
                0, 0, 0, "x", 0, items));
    }

    @Test
    void everyPlayerWithDeathsIsAHeadWithTheirNameAndCount() {
        died(ANA, "Ana", 0);
        died(ANA, "Ana", 10);
        died(BEA, "Bea", 30);
        DeathPlayersMenu menu = new DeathPlayersMenu(ui, service, view, null);

        UiTesting.drawAsChest(menu);

        ItemStack first = UiTesting.itemAt(menu, 10).stack();
        assertEquals(Items.PLAYER_HEAD, first.getItem());
        assertEquals("Bea", first.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(List.of(
                        "Muertes guardadas: 1",
                        "Última: " + Timestamps.dateTime(WHEN.plusSeconds(1800)),
                        "",
                        "◀ Clic Izq. para ver muertes"),
                first.get(DataComponents.LORE).lines().stream().map(Component::getString).toList());
        assertEquals("Ana", UiTesting.itemAt(menu, 11).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Muertes guardadas: 2",
                UiTesting.itemAt(menu, 11).stack().get(DataComponents.LORE).lines().getFirst().getString());
    }

    @Test
    void withoutDeathsTheListIsEmpty() {
        DeathPlayersMenu menu = new DeathPlayersMenu(ui, service, view, null);

        UiTesting.drawAsChest(menu);

        assertEquals(Items.PAPER, UiTesting.itemAt(menu, 22).stack().getItem());
    }
}
