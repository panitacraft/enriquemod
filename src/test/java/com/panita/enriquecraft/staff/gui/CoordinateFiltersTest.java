package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.ui.UiPagedMenu.Filter;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateFiltersTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID ANA = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID BEN = UUID.fromString("66666666-7777-8888-9999-000000000000");

    @TempDir
    Path directory;

    private MenuFactory factory;

    @BeforeEach
    void createFactory() {
        factory = MinecraftTestSupport.menuFactory(directory);
    }

    private static SavedCoordinate coordinate(String name, ResourceKey<Level> dimension, UUID by, String byName, String when) {
        return new SavedCoordinate(name, dimension, 0, 64, 0, 0, 0, by, byName, Instant.parse(when), Items.COMPASS);
    }

    private List<Filter<SavedCoordinate>> filters(SavedCoordinate... coordinates) {
        return CoordinateFilters.of(List.of(coordinates), factory, NOW);
    }

    private static Filter<SavedCoordinate> named(List<Filter<SavedCoordinate>> filters, String text) {
        return filters.stream().filter(filter -> filter.name().getString().equals(text)).findFirst().orElseThrow();
    }

    @Test
    void onlyDimensionsInUseAreOfferedInTheirUsualOrder() {
        List<String> names = filters(
                coordinate("a", Level.END, ANA, "Ana", "2026-10-04T10:00:00Z"),
                coordinate("b", Level.OVERWORLD, ANA, "Ana", "2026-10-04T10:00:00Z"))
                .stream().map(filter -> filter.name().getString()).toList();

        assertEquals("Dimensión: Overworld", names.get(0));
        assertEquals("Dimensión: End", names.get(1));
        assertFalse(names.contains("Dimensión: Nether"));
    }

    @Test
    void anotherDimensionComesAfterTheVanillaOnes() {
        ResourceKey<Level> moon = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("mymod", "moon"));

        List<String> names = filters(
                coordinate("a", moon, ANA, "Ana", "2026-10-04T10:00:00Z"),
                coordinate("b", Level.NETHER, ANA, "Ana", "2026-10-04T10:00:00Z"))
                .stream().map(filter -> filter.name().getString()).toList();

        assertEquals(List.of("Dimensión: Nether", "Dimensión: mymod:moon"), names.subList(0, 2));
    }

    @Test
    void aDimensionFilterKeepsOnlyThatDimension() {
        SavedCoordinate nether = coordinate("a", Level.NETHER, ANA, "Ana", "2026-10-04T10:00:00Z");
        SavedCoordinate end = coordinate("b", Level.END, ANA, "Ana", "2026-10-04T10:00:00Z");

        Filter<SavedCoordinate> filter = named(filters(nether, end), "Dimensión: Nether");

        assertTrue(filter.keeps().test(nether));
        assertFalse(filter.keeps().test(end));
    }

    @Test
    void theDateFiltersKeepWhatWasSavedWithinTheirSpan() {
        SavedCoordinate today = coordinate("a", Level.OVERWORLD, ANA, "Ana", "2026-10-04T01:00:00Z");
        SavedCoordinate lastWeek = coordinate("b", Level.OVERWORLD, ANA, "Ana", "2026-09-30T12:00:00Z");
        SavedCoordinate lastMonth = coordinate("c", Level.OVERWORLD, ANA, "Ana", "2026-09-10T12:00:00Z");
        SavedCoordinate old = coordinate("d", Level.OVERWORLD, ANA, "Ana", "2026-01-01T12:00:00Z");
        List<Filter<SavedCoordinate>> filters = filters(today, lastWeek, lastMonth, old);

        Filter<SavedCoordinate> day = named(filters, "Fecha: hoy");
        Filter<SavedCoordinate> week = named(filters, "Fecha: últimos 7 días");
        Filter<SavedCoordinate> month = named(filters, "Fecha: últimos 30 días");

        assertEquals(List.of(true, false, false, false), List.of(today, lastWeek, lastMonth, old).stream().map(day.keeps()::test).toList());
        assertEquals(List.of(true, true, false, false), List.of(today, lastWeek, lastMonth, old).stream().map(week.keeps()::test).toList());
        assertEquals(List.of(true, true, true, false), List.of(today, lastWeek, lastMonth, old).stream().map(month.keeps()::test).toList());
    }

    @Test
    void eachPersonWhoSavedACoordinateIsOfferedOnceByName() {
        List<String> names = filters(
                coordinate("a", Level.OVERWORLD, BEN, "Ben", "2026-10-04T10:00:00Z"),
                coordinate("b", Level.OVERWORLD, ANA, "Ana", "2026-10-04T10:00:00Z"),
                coordinate("c", Level.OVERWORLD, ANA, "Ana", "2026-10-04T11:00:00Z"))
                .stream().map(filter -> filter.name().getString()).filter(name -> name.startsWith("Usuario")).toList();

        assertEquals(List.of("Usuario: Ana", "Usuario: Ben"), names);
    }

    @Test
    void aPersonFilterKeepsWhatThatPersonSaved() {
        SavedCoordinate byAna = coordinate("a", Level.OVERWORLD, ANA, "Ana", "2026-10-04T10:00:00Z");
        SavedCoordinate byBen = coordinate("b", Level.OVERWORLD, BEN, "Ben", "2026-10-04T10:00:00Z");

        Filter<SavedCoordinate> filter = named(filters(byAna, byBen), "Usuario: Ben");

        assertFalse(filter.keeps().test(byAna));
        assertTrue(filter.keeps().test(byBen));
    }

    @Test
    void aNameChangeIsFollowedByTheMostRecentName() {
        List<String> names = filters(
                coordinate("a", Level.OVERWORLD, ANA, "AnaVieja", "2026-10-01T10:00:00Z"),
                coordinate("b", Level.OVERWORLD, ANA, "AnaNueva", "2026-10-04T10:00:00Z"))
                .stream().map(filter -> filter.name().getString()).filter(name -> name.startsWith("Usuario")).toList();

        assertEquals(List.of("Usuario: AnaNueva"), names);
    }

    @Test
    void anEmptyListStillOffersTheDateFilters() {
        List<Component> names = filters().stream().map(Filter::name).toList();

        assertEquals(3, names.size());
    }
}
