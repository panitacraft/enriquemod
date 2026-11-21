package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.item.CustomItemTag;
import com.panita.enriquecraft.staff.data.SavedItem;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemServiceTest {

    private static final UUID STAFF = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Instant NOW = Instant.parse("2026-09-30T04:12:00Z");

    @TempDir
    Path directory;

    private CustomItemService service;

    @BeforeEach
    void createService() {
        WorldData worldData = new WorldData();
        worldData.attach(directory, MinecraftTestSupport.ops());
        service = new CustomItemService(worldData);
    }

    private static ItemStack sword() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Espada de prueba"));
        sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Una línea"))));
        return sword;
    }

    private AddResultHolder save(ItemStack held, String name) {
        return new AddResultHolder(service.save(held, name, STAFF, "Ana", NOW));
    }

    private record AddResultHolder(CustomItemService.AddResult result) {
    }

    @Test
    void savingStoresACopyMarkedWithTheNameAndMarksTheHeldItemToo() {
        ItemStack held = sword();

        assertEquals(CustomItemService.AddResult.ADDED, save(held, "espada").result());

        assertTrue(CustomItemTag.is(held, "espada"), "the held item is marked");
        SavedItem saved = service.find("espada").orElseThrow();
        assertTrue(CustomItemTag.is(saved.stack(), "espada"), "the saved item is marked");
        assertTrue(ItemStack.matches(held, saved.stack()), "held and saved items are the same");
        assertEquals("Ana", saved.savedByName());
        assertEquals(STAFF, saved.savedBy());
        assertEquals(NOW, saved.savedAt());
    }

    @Test
    void theSavedItemDoesNotChangeWhenTheHeldOneDoes() {
        ItemStack held = sword();
        save(held, "espada");

        held.setCount(7);
        held.set(DataComponents.CUSTOM_NAME, Component.literal("Otro nombre"));

        SavedItem saved = service.find("espada").orElseThrow();
        assertEquals(1, saved.stack().getCount());
        assertEquals("Espada de prueba", saved.stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void theCountIsKeptAsHeld() {
        ItemStack held = new ItemStack(Items.ARROW, 16);

        save(held, "flechas");

        assertEquals(16, service.find("flechas").orElseThrow().stack().getCount());
    }

    @Test
    void invalidNamesAreRejectedAndTheHeldItemIsLeftAlone() {
        for (String name : List.of("", "Upper", "with space", "a".repeat(33), "colon:name")) {
            ItemStack held = sword();

            assertEquals(CustomItemService.AddResult.INVALID_NAME, save(held, name).result(), "'" + name + "'");
            assertEquals(Optional.empty(), CustomItemTag.nameOf(held), "'" + name + "' must not mark the item");
        }
        assertTrue(service.all().isEmpty());
    }

    @Test
    void aDuplicateIsRejectedAndTheHeldItemIsLeftAlone() {
        save(sword(), "espada");
        ItemStack another = new ItemStack(Items.DIAMOND_AXE);

        assertEquals(CustomItemService.AddResult.DUPLICATE, save(another, "espada").result());

        assertEquals(Optional.empty(), CustomItemTag.nameOf(another));
        assertEquals(1, service.all().size());
        assertEquals(Items.DIAMOND_SWORD, service.find("espada").orElseThrow().stack().getItem());
    }

    @Test
    void lookupsIgnoreTheCaseOfTheInput() {
        save(sword(), "espada");

        assertTrue(service.find("ESPADA").isPresent());
        assertTrue(service.remove("Espada"));
        assertFalse(service.find("espada").isPresent());
    }

    @Test
    void namesAreSorted() {
        save(new ItemStack(Items.STICK), "zeta");
        save(new ItemStack(Items.STICK), "alfa");
        save(new ItemStack(Items.STICK), "beta");

        assertEquals(List.of("alfa", "beta", "zeta"), service.names());
    }

    @Test
    void removingAMissingItemReportsIt() {
        assertFalse(service.remove("nada"));
    }

    @Test
    void itemsSurviveARestartWithEveryComponent() {
        ItemStack held = sword();
        held.set(DataComponents.MAX_DAMAGE, 999);
        save(held, "espada");

        WorldData restarted = new WorldData();
        CustomItemService reloaded = new CustomItemService(restarted);
        restarted.attach(directory, MinecraftTestSupport.ops());

        SavedItem saved = reloaded.find("espada").orElseThrow();
        assertTrue(ItemStack.matches(held, saved.stack()), "every component survived the file");
        assertTrue(CustomItemTag.is(saved.stack(), "espada"));
    }
}
