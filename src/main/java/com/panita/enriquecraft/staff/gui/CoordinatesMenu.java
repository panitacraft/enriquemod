package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every saved coordinate as an icon, searchable by name and filterable by dimension, date or person.
 * Hovering shows its name and dimension; left click teleports there, and right click opens everything
 * about it.
 */
public final class CoordinatesMenu extends UiPagedMenu<SavedCoordinate> {

    private final CoordinateService service;
    private final CoordinateView view;
    private final Clock clock;

    public CoordinatesMenu(UiService ui, CoordinateService service, CoordinateView view) {
        this(ui, service, view, null, Clock.systemDefaultZone());
    }

    /** A list that goes back to the menu it was opened from. */
    public CoordinatesMenu(UiService ui, CoordinateService service, CoordinateView view, UiMenu previous) {
        this(ui, service, view, previous, Clock.systemDefaultZone());
    }

    CoordinatesMenu(UiService ui, CoordinateService service, CoordinateView view, Clock clock) {
        this(ui, service, view, null, clock);
    }

    private CoordinatesMenu(UiService ui, CoordinateService service, CoordinateView view, UiMenu previous, Clock clock) {
        super(ui, previous);
        this.service = service;
        this.view = view;
        this.clock = clock;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Coordinates.MENU_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.COMPASS);
    }

    @Override
    protected List<SavedCoordinate> entries() {
        return service.all();
    }

    /** Both what staff see and the id they type in commands can be searched. */
    @Override
    protected Optional<Function<SavedCoordinate, String>> searchText() {
        return Optional.of(coordinate -> coordinate.displayName() + " " + coordinate.name());
    }

    @Override
    protected List<Filter<SavedCoordinate>> filters() {
        return CoordinateFilters.of(service.all(), factory(), clock);
    }

    @Override
    protected UiElement render(UiBuilder builder, SavedCoordinate coordinate) {
        Component name = factory().text(
                Message.plain(StaffMessages.Coordinates.ENTRY_NAME).with("name", coordinate.displayName()));
        Component dimension = factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DIMENSION)
                .with("dimension", Dimensions.coloredName(coordinate.dimension())));
        if (arranging()) {
            return arrangeable(builder, coordinate, name, dimension);
        }
        // Only what tells coordinates apart at a glance; the rest is one right click away.
        List<Component> tooltip = List.of(
                dimension,
                factory().text(Message.plain("")),
                ClickHints.left(factory(), "ir"),
                ClickHints.right(factory(), "info"));
        return builder.button(coordinate.iconStack(), name, tooltip, click -> {
            if (click.isLeft()) {
                ui().close(click.player());
                view.teleport(click.player(), coordinate);
            } else if (click.isRight()) {
                new CoordinateDetailMenu(ui(), service, view, coordinate.name(), this).open(click.player());
            }
        });
    }

    /** What a chest shows while arranging: a click lifts a coordinate, then places it in another's spot. */
    private UiElement arrangeable(UiBuilder builder, SavedCoordinate coordinate, Component name, Component dimension) {
        String action = isLifted(coordinate) ? "soltar" : hasLifted() ? "colocar aquí" : "levantar";
        List<Component> tooltip = List.of(dimension, factory().text(Message.plain("")), ClickHints.left(factory(), action));
        return builder.button(ButtonRole.NONE, coordinate.iconStack(), name, tooltip, isLifted(coordinate) ? "↕" : "", click -> {
            if (click.isLeft()) {
                arrange(coordinate);
            }
        });
    }

    @Override
    protected boolean reorderable() {
        return true;
    }

    @Override
    protected void move(SavedCoordinate dragged, SavedCoordinate onto) {
        service.move(dragged.name(), onto.name());
    }
}
