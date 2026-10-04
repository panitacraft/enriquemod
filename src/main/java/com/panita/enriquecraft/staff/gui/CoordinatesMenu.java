package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
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
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every saved coordinate as an icon, searchable by name. Hovering shows its name and dimension; left
 * click teleports there, and right click opens everything about it.
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
    protected ItemStack icon() {
        return new ItemStack(Items.COMPASS);
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
        // Only what tells coordinates apart at a glance; the rest is one right click away.
        List<Component> tooltip = List.of(
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DIMENSION_ONLY)
                        .with("dimension", Dimensions.coloredName(coordinate.dimension()))),
                factory().text(Message.plain("")),
                factory().text(StaffMessages.Coordinates.ENTRY_GO_HINT),
                factory().text(StaffMessages.Coordinates.ENTRY_DETAILS_HINT));
        return builder.button(new ItemStack(coordinate.icon()), name, tooltip, click -> {
            if (click.isLeft()) {
                ui().close(click.player());
                view.teleport(click.player(), coordinate);
            } else if (click.isRight()) {
                new CoordinateDetailMenu(ui(), service, view, coordinate.name(), this).open(click.player());
            }
        });
    }
}
