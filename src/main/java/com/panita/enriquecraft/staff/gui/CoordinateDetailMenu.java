package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.ConfirmMenu;
import com.panita.enriquecraft.core.ui.PlayerHeads;
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
 * One saved coordinate in full: its icon, which can be pressed to choose another one, everything known
 * about it, the name staff see, which can be edited, and the actions on it: going there and deleting it,
 * after confirming. The coordinate is read again on every redraw, so a change shows as soon as it is made.
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

    /** A small screen that reads as one block, with the controls on its last row. */
    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        Optional<SavedCoordinate> found = service.find(name);
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR),
                factory().text(Messages.Gui.BACK), List.of(), click -> previous().open(click.player()));
        if (found.isEmpty()) {
            UiElement missing = new UiElement.Detail(new ItemStack(Items.BARRIER),
                    factory().text(Message.plain(StaffMessages.Coordinates.NOT_FOUND).with("name", name)), List.of());
            // Same number of rows as the full view, since a chest cannot change size while it is open.
            return new UiElement.Column(List.of(missing, new UiElement.Row(List.of()), new UiElement.Row(List.of(back))));
        }

        SavedCoordinate coordinate = found.get();
        UiElement detail = builder.detail(new ItemStack(coordinate.icon()),
                factory().text(Message.plain(StaffMessages.Coordinates.DETAIL_NAME).with("name", coordinate.displayName())),
                lines(coordinate), List.of(ClickHints.left(factory(), "cambiar icono")), click -> {
                    if (click.isLeft()) {
                        new CoordinateIconMenu(ui(), service, coordinate.name(), this).open(click.player());
                    }
                });
        UiElement displayName = builder.input(factory().text(StaffMessages.Coordinates.DETAIL_NAME_HINT),
                coordinate.displayName(), SavedCoordinate.MAX_DISPLAY_NAME, submit -> {
                    service.updateDisplayName(coordinate.name(), submit.text());
                    refresh();
                });
        UiElement teleport = builder.button(new ItemStack(Items.ENDER_PEARL),
                factory().text(StaffMessages.Coordinates.DETAIL_TELEPORT),
                List.of(factory().text(StaffMessages.Coordinates.DETAIL_TELEPORT_LORE)), click -> {
                    if (click.isLeft()) {
                        ui().close(click.player());
                        view.teleport(click.player(), coordinate);
                    }
                });
        UiElement delete = builder.button(new ItemStack(Items.LAVA_BUCKET),
                factory().text(StaffMessages.Coordinates.DETAIL_DELETE),
                List.of(factory().text(StaffMessages.Coordinates.DETAIL_DELETE_LORE)), click -> {
                    if (click.isLeft()) {
                        confirmDelete(coordinate).open(click.player());
                    }
                });
        UiElement none = new UiElement.Spacer();
        return new UiElement.Column(List.of(
                detail,
                displayName,
                new UiElement.Row(List.of(back, none, none, teleport, delete, none, none, none, none))));
    }

    private ConfirmMenu confirmDelete(SavedCoordinate coordinate) {
        return new ConfirmMenu(ui(), this, factory().text(StaffMessages.Coordinates.DELETE_TITLE),
                new ItemStack(coordinate.icon()),
                factory().text(Message.plain(StaffMessages.Coordinates.DELETE_HEADLINE).with("name", coordinate.displayName())),
                List.of(factory().text(StaffMessages.Coordinates.DELETE_WARNING)),
                player -> {
                    service.remove(coordinate.name());
                    previous().open(player);
                });
    }

    private List<Component> lines(SavedCoordinate coordinate) {
        return List.of(
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_ID).with("id", coordinate.name())),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DIMENSION)
                        .with("dimension", Dimensions.coloredName(coordinate.dimension()))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_POSITION)
                        .with("x", CoordinateView.block(coordinate.x()))
                        .with("y", CoordinateView.block(coordinate.y()))
                        .with("z", CoordinateView.block(coordinate.z()))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_SAVED_BY)
                        .with("player", PlayerHeads.inline(coordinate.savedBy(), Component.literal(coordinate.savedByName())))),
                factory().text(Message.plain(StaffMessages.Coordinates.ENTRY_DATE)
                        .with("date", Timestamps.date(coordinate.savedAt()))));
    }
}
