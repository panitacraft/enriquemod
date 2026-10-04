package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every saved coordinate as an icon with its details, searchable by name. Left click teleports there.
 */
public final class CoordinatesMenu extends UiPagedMenu<SavedCoordinate> {

    private final CoordinateService service;
    private final CoordinateView view;

    public CoordinatesMenu(UiService ui, CoordinateService service, CoordinateView view) {
        super(ui, null);
        this.service = service;
        this.view = view;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Coordinates.MENU_TITLE);
    }

    @Override
    protected List<SavedCoordinate> entries() {
        return service.all();
    }

    @Override
    protected Optional<Function<SavedCoordinate, String>> searchText() {
        return Optional.of(SavedCoordinate::name);
    }

    @Override
    protected UiElement render(UiBuilder builder, SavedCoordinate coordinate) {
        Component name = factory().text(
                Message.plain(StaffMessages.Coordinates.ENTRY_NAME).with("name", coordinate.name()));
        List<Component> details = List.of(
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DIMENSION)
                        .with("dimension", Dimensions.displayName(coordinate.dimension()))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_POSITION)
                        .with("x", CoordinateView.number(coordinate.x()))
                        .with("y", CoordinateView.number(coordinate.y()))
                        .with("z", CoordinateView.number(coordinate.z()))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_SAVED_BY)
                        .with("player", coordinate.savedByName())),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DATE)
                        .with("date", Timestamps.format(coordinate.savedAt()))),
                factory().text(StaffMessages.Coordinates.ENTRY_CLICK_HINT));
        return builder.button(new ItemStack(coordinate.icon()), name, details, click -> {
            if (click.isLeft()) {
                ui().close(click.player());
                view.teleport(click.player(), coordinate);
            }
        });
    }
}
