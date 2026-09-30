package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.gui.PaginatedMenu;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Every saved coordinate as an icon with its details. Left click teleports there.
 */
public final class CoordinatesMenu extends PaginatedMenu<SavedCoordinate> {

    private final CoordinateService service;
    private final CoordinateView view;

    public CoordinatesMenu(MenuFactory factory, CoordinateService service, CoordinateView view) {
        super(factory, null);
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
    protected MenuItem render(SavedCoordinate coordinate) {
        ItemStack stack = factory().item(coordinate.icon())
                .name(Message.plain(StaffMessages.Coordinates.ENTRY_NAME).with("name", coordinate.name()))
                .lore(Message.plain(StaffMessages.Coordinates.ENTRY_DIMENSION)
                                .with("dimension", Dimensions.displayName(coordinate.dimension())),
                        Message.plain(StaffMessages.Coordinates.ENTRY_POSITION)
                                .with("x", CoordinateView.number(coordinate.x()))
                                .with("y", CoordinateView.number(coordinate.y()))
                                .with("z", CoordinateView.number(coordinate.z())),
                        Message.plain(StaffMessages.Coordinates.ENTRY_SAVED_BY).with("player", coordinate.savedByName()),
                        Message.plain(StaffMessages.Coordinates.ENTRY_DATE).with("date", Timestamps.format(coordinate.savedAt())),
                        Message.plain(StaffMessages.Coordinates.ENTRY_CLICK_HINT))
                .build();
        return MenuItem.button(stack, click -> {
            if (click.isLeft()) {
                click.player().closeContainer();
                view.teleport(click.player(), coordinate);
            }
        });
    }
}
