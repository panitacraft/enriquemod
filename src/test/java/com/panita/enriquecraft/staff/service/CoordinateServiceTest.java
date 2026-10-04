package com.panita.enriquecraft.staff.service;

import net.minecraft.nbt.NbtOps;
import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateServiceTest {

    @TempDir
    Path directory;

    private WorldData worldData;
    private CoordinateService service;

    @BeforeEach
    void createService() {
        MinecraftTestSupport.bootstrap();
        worldData = new WorldData();
        worldData.attach(directory, NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
    }

    private static SavedCoordinate coordinate(String name) {
        return new SavedCoordinate(name, Level.NETHER, 10.5, 64.0, -20.25, 90.0F, 15.0F, UUID.randomUUID(), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), Items.COMPASS);
    }

    @Test
    void addedCoordinateCanBeFoundIgnoringCase() {
        assertEquals(CoordinateService.AddResult.ADDED, service.add(coordinate("Base")));

        assertTrue(service.find("base").isPresent());
        assertTrue(service.find("BASE").isPresent());
        assertFalse(service.find("other").isPresent());
    }

    @Test
    void duplicateNamesAreRejectedIgnoringCase() {
        service.add(coordinate("Base"));

        assertEquals(CoordinateService.AddResult.DUPLICATE, service.add(coordinate("base")));
        assertEquals(1, service.all().size());
    }

    @Test
    void invalidNamesAreRejected() {
        for (String name : List.of("", "with space", "a".repeat(33), "dots.here", "ñandú", "a/b")) {
            assertEquals(CoordinateService.AddResult.INVALID_NAME, service.add(coordinate(name)), "'" + name + "'");
        }
        assertTrue(service.all().isEmpty());
    }

    @Test
    void validNames() {
        for (String name : List.of("a", "Base_1", "mina-de-oro", "A".repeat(32))) {
            assertTrue(CoordinateService.isValidName(name), name);
        }
    }

    @Test
    void allAreSortedByNameIgnoringCase() {
        service.add(coordinate("zeta"));
        service.add(coordinate("Alfa"));
        service.add(coordinate("beta"));

        assertEquals(List.of("Alfa", "beta", "zeta"), service.names());
    }

    @Test
    void removeDeletesIgnoringCaseAndReportsMissing() {
        service.add(coordinate("Base"));

        assertTrue(service.remove("BASE"));
        assertFalse(service.remove("Base"));
        assertTrue(service.all().isEmpty());
    }

    @Test
    void coordinatesSurviveARestartAndKeepEveryField() {
        SavedCoordinate saved = coordinate("Base");
        service.add(saved);

        WorldData restarted = new WorldData();
        CoordinateService reloaded = new CoordinateService(restarted);
        restarted.attach(directory, NbtOps.INSTANCE);

        assertEquals(List.of(saved), reloaded.all());
    }

    @Test
    void updatingTheIconChangesOnlyThatCoordinate() {
        service.add(coordinate("base"));
        service.add(coordinate("mina"));

        assertTrue(service.updateIcon("BASE", Items.DIAMOND));

        assertEquals(Items.DIAMOND, service.find("base").orElseThrow().icon());
        assertEquals(Items.COMPASS, service.find("mina").orElseThrow().icon());
        assertEquals("base", service.find("base").orElseThrow().name());
    }

    @Test
    void updatingTheIconOfAMissingCoordinateDoesNothing() {
        assertFalse(service.updateIcon("nope", Items.DIAMOND));
    }

    @Test
    void updatingTheDisplayNameKeepsTheIdAndTrims() {
        service.add(coordinate("base"));

        assertTrue(service.updateDisplayName("BASE", "  Mi base  "));

        assertEquals("Mi base", service.find("base").orElseThrow().displayName());
        assertEquals("base", service.find("base").orElseThrow().name());
    }

    @Test
    void anInvalidDisplayNameIsRefusedAndNothingChanges() {
        service.add(coordinate("base"));

        assertFalse(service.updateDisplayName("base", "   "));
        assertFalse(service.updateDisplayName("base", "x".repeat(33)));

        assertEquals("base", service.find("base").orElseThrow().displayName());
    }

    @Test
    void theDisplayNameOfAMissingCoordinateCannotBeChanged() {
        assertFalse(service.updateDisplayName("nope", "Algo"));
    }

    @Test
    void aDisplayNameSurvivesReloadingTheFile() {
        service.add(coordinate("base"));
        service.updateDisplayName("base", "Mi base");

        CoordinateService reloaded = new CoordinateService(worldData);

        assertEquals("Mi base", reloaded.find("base").orElseThrow().displayName());
    }
}
