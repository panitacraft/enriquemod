package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The coordinates menu with its search box and filter dropdown. */
class CoordinatesMenuFilterTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path directory;

    private CoordinateService service;
    private CoordinatesMenu menu;

    @BeforeEach
    void create() {
        UiService ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
        CoordinateView view = new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service);
        menu = new CoordinatesMenu(ui, service, view, NOW);
    }

    private void save(String name, ResourceKey<Level> dimension, String when) {
        service.add(new SavedCoordinate(name, dimension, 0, 64, 0, 0, 0, UUID.randomUUID(), "Ana", Instant.parse(when),
                Items.COMPASS));
    }

    private String nameAt(int slot) {
        return UiTesting.itemAt(menu, slot).stack().get(DataComponents.CUSTOM_NAME).getString();
    }

    @Test
    void theDimensionFilterNarrowsTheList() {
        save("alfa", Level.OVERWORLD, "2026-10-04T10:00:00Z");
        save("beta", Level.NETHER, "2026-10-04T10:00:00Z");
        UiTesting.drawAsChest(menu);

        UiTesting.select(menu, 2);

        assertEquals("→ beta", nameAt(10));
        assertNull(UiTesting.itemAt(menu, 11));
    }

    @Test
    void searchingMatchesTheDisplayNameAndTheId() {
        save("alfa", Level.OVERWORLD, "2026-10-04T10:00:00Z");
        save("beta", Level.OVERWORLD, "2026-10-04T10:00:00Z");
        service.updateDisplayName("beta", "Casa del lago");
        UiTesting.drawAsChest(menu);

        UiTesting.submit(menu, "lago");
        assertEquals("→ Casa del lago", nameAt(10));
        assertNull(UiTesting.itemAt(menu, 11));

        UiTesting.submit(menu, "beta");
        assertEquals("→ Casa del lago", nameAt(10));
    }

    @Test
    void theListShowsTheDisplayNameNotTheId() {
        save("alfa", Level.OVERWORLD, "2026-10-04T10:00:00Z");
        service.updateDisplayName("alfa", "Mina de hierro");

        UiTesting.drawAsChest(menu);

        assertEquals("→ Mina de hierro", nameAt(10));
    }
}
