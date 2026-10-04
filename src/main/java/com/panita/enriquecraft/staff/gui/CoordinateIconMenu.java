package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateIcons;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Every icon a coordinate can have. Left click chooses one and goes back to the coordinate; the
 * current icon shines.
 */
public final class CoordinateIconMenu extends UiPagedMenu<Item> {

    private final CoordinateService service;
    private final String name;

    public CoordinateIconMenu(UiService ui, CoordinateService service, String name, UiMenu previous) {
        super(ui, previous);
        this.service = service;
        this.name = name;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Coordinates.ICON_MENU_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.PAINTING);
    }

    @Override
    protected List<Item> entries() {
        return CoordinateIcons.all();
    }

    @Override
    protected UiElement render(UiBuilder builder, Item item) {
        boolean current = service.find(name).map(SavedCoordinate::icon).filter(item::equals).isPresent();
        ItemStack shown = current ? factory().item(item).glint().build() : new ItemStack(item);
        Component hint = current ? factory().text(StaffMessages.Coordinates.ICON_CURRENT) : ClickHints.left(factory(), "elegir");
        // No label: the button is the item itself, so it keeps its own name.
        return builder.button(shown, Component.empty(), List.of(hint), click -> {
            if (click.isLeft()) {
                service.updateIcon(name, item);
                previous().open(click.player());
            }
        });
    }
}
