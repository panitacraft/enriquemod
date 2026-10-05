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
 * The map of each kind of structure, so a coordinate can be told apart by the structure it marks. Choosing one
 * sets it as the icon and goes back to the coordinate; the current one shines.
 */
final class CoordinateMapMenu extends UiPagedMenu<Item> {

    private final CoordinateService service;
    private final String name;
    private final UiMenu coordinateMenu;

    /**
     * @param previous       the icon list this was opened from, which back returns to
     * @param coordinateMenu the coordinate's own screen, where choosing an icon ends
     */
    CoordinateMapMenu(UiService ui, CoordinateService service, String name, UiMenu previous, UiMenu coordinateMenu) {
        super(ui, previous);
        this.service = service;
        this.name = name;
        this.coordinateMenu = coordinateMenu;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Coordinates.MAPS_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.ABANDONED_CAMP_MAP);
    }

    @Override
    protected List<Item> entries() {
        return CoordinateIcons.structureMaps();
    }

    @Override
    protected UiElement render(UiBuilder builder, Item map) {
        boolean current = service.find(name).filter(coordinate -> coordinate.iconPlayer().isEmpty())
                .map(SavedCoordinate::icon).filter(map::equals).isPresent();
        ItemStack shown = current ? factory().item(map).glint().build() : new ItemStack(map);
        Component hint = current ? factory().text(StaffMessages.Coordinates.ICON_CURRENT) : ClickHints.left(factory(), "elegir");
        // No label: the map keeps its own name, which says which structure it leads to.
        return builder.button(shown, Component.empty(), List.of(hint), click -> {
            if (click.isLeft()) {
                service.updateIcon(name, map);
                coordinateMenu.open(click.player());
            }
        });
    }
}
