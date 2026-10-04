package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
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

/**
 * One saved coordinate in full: its icon with everything known about it, and the actions on it,
 * which are going there and choosing another icon. The coordinate is read again on every redraw, so a
 * new icon shows as soon as it is chosen.
 */
public final class CoordinateDetailMenu extends UiMenu {

    private final CoordinateService service;
    private final CoordinateView view;
    private final String name;

    public CoordinateDetailMenu(UiService ui, CoordinateService service, CoordinateView view, String name,
                                UiMenu previous) {
        super(ui, previous);
        this.service = service;
        this.view = view;
        this.name = name;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Coordinates.DETAIL_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.COMPASS);
    }

    /** A small screen that reads as one block, with the controls on its second row. */
    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        Optional<SavedCoordinate> found = service.find(name);
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR),
                factory().text(StaffMessages.Coordinates.BACK), List.of(), click -> previous().open(click.player()));
        UiElement none = new UiElement.Spacer();
        if (found.isEmpty()) {
            UiElement missing = new UiElement.Detail(new ItemStack(Items.BARRIER),
                    factory().text(Message.plain(StaffMessages.Coordinates.NOT_FOUND).with("name", name)), List.of());
            return new UiElement.Column(List.of(missing, new UiElement.Row(List.of(back))));
        }

        SavedCoordinate coordinate = found.get();
        UiElement changeIcon = builder.button(new ItemStack(coordinate.icon()),
                factory().text(StaffMessages.Coordinates.DETAIL_CHANGE_ICON),
                List.of(factory().text(StaffMessages.Coordinates.DETAIL_CHANGE_ICON_LORE)), click -> {
                    if (click.isLeft()) {
                        new CoordinateIconMenu(ui(), service, coordinate.name(), this).open(click.player());
                    }
                });
        UiElement teleport = builder.button(new ItemStack(Items.ENDER_PEARL),
                factory().text(StaffMessages.Coordinates.DETAIL_TELEPORT),
                List.of(factory().text(StaffMessages.Coordinates.DETAIL_TELEPORT_LORE)), click -> {
                    if (click.isLeft()) {
                        ui().close(click.player());
                        view.teleport(click.player(), coordinate);
                    }
                });
        return new UiElement.Column(List.of(
                new UiElement.Detail(new ItemStack(coordinate.icon()),
                        factory().text(Message.plain(StaffMessages.Coordinates.DETAIL_NAME).with("name", coordinate.name())),
                        lines(coordinate)),
                new UiElement.Row(List.of(back, none, none, changeIcon, none, teleport, none, none, none))));
    }

    private List<Component> lines(SavedCoordinate coordinate) {
        return List.of(
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DIMENSION)
                        .with("dimension", Dimensions.coloredName(coordinate.dimension()))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_POSITION)
                        .with("x", CoordinateView.number(coordinate.x()))
                        .with("y", CoordinateView.number(coordinate.y()))
                        .with("z", CoordinateView.number(coordinate.z()))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_SAVED_BY)
                        .with("player", coordinate.savedByName())),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DATE)
                        .with("date", Timestamps.date(coordinate.savedAt()))));
    }
}
