package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.ui.UiPagedMenu.Filter;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.StaffMessages;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ways the list of coordinates can be narrowed: to one dimension, to those saved recently, or to
 * those saved by one person. Only dimensions and people that appear in the list are offered, so no
 * option leads to an empty list.
 */
final class CoordinateFilters {

    private static final List<ResourceKey<Level>> VANILLA_ORDER = List.of(Level.OVERWORLD, Level.NETHER, Level.END);

    private CoordinateFilters() {
    }

    static List<Filter<SavedCoordinate>> of(List<SavedCoordinate> all, MenuFactory factory, Clock clock) {
        List<Filter<SavedCoordinate>> filters = new ArrayList<>();

        for (ResourceKey<Level> dimension : dimensionsIn(all)) {
            filters.add(new Filter<>(factory.text(Message.plain(StaffMessages.Coordinates.FILTER_DIMENSION)
                    .with("dimension", Dimensions.coloredName(dimension))),
                    coordinate -> coordinate.dimension().equals(dimension)));
        }

        LocalDate today = LocalDate.now(clock);
        filters.add(new Filter<>(factory.text(StaffMessages.Coordinates.FILTER_TODAY),
                coordinate -> LocalDate.ofInstant(coordinate.savedAt(), clock.getZone()).equals(today)));
        filters.add(new Filter<>(factory.text(StaffMessages.Coordinates.FILTER_WEEK),
                coordinate -> savedWithin(coordinate, clock, Duration.ofDays(7))));
        filters.add(new Filter<>(factory.text(StaffMessages.Coordinates.FILTER_MONTH),
                coordinate -> savedWithin(coordinate, clock, Duration.ofDays(30))));

        usersIn(all).forEach((user, name) -> filters.add(new Filter<>(
                factory.text(Message.plain(StaffMessages.Coordinates.FILTER_USER).with("player", name)),
                coordinate -> coordinate.savedBy().equals(user))));
        return filters;
    }

    private static boolean savedWithin(SavedCoordinate coordinate, Clock clock, Duration span) {
        Instant limit = clock.instant().minus(span);
        return !coordinate.savedAt().isBefore(limit);
    }

    /** The dimensions in use: the three vanilla ones first, in their usual order, then any other by identifier. */
    private static List<ResourceKey<Level>> dimensionsIn(List<SavedCoordinate> all) {
        return all.stream().map(SavedCoordinate::dimension).distinct()
                .sorted(Comparator.comparingInt((ResourceKey<Level> dimension) -> {
                    int index = VANILLA_ORDER.indexOf(dimension);
                    return index < 0 ? VANILLA_ORDER.size() : index;
                }).thenComparing(dimension -> dimension.identifier().toString()))
                .toList();
    }

    /** Each person who saved a coordinate, by name, once; the most recent name wins if it changed. */
    private static Map<UUID, String> usersIn(List<SavedCoordinate> all) {
        Map<UUID, SavedCoordinate> latest = new LinkedHashMap<>();
        for (SavedCoordinate coordinate : all) {
            latest.merge(coordinate.savedBy(), coordinate,
                    (kept, other) -> other.savedAt().isAfter(kept.savedAt()) ? other : kept);
        }
        Map<UUID, String> users = new LinkedHashMap<>();
        latest.values().stream()
                .sorted(Comparator.comparing(coordinate -> coordinate.savedByName().toLowerCase()))
                .forEach(coordinate -> users.put(coordinate.savedBy(), coordinate.savedByName()));
        return users;
    }
}
