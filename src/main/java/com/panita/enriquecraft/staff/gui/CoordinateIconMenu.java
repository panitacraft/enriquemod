package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.PlayerRef;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateIcons;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every icon a coordinate can have. The first two entries lead to more choices: the head of a player, whose
 * name is asked for, and a map for each kind of structure. Left click on any other entry chooses it and goes
 * back to the coordinate; the current icon shines.
 */
public final class CoordinateIconMenu extends UiPagedMenu<CoordinateIconMenu.Choice> {

    /** One entry of the list: an ordinary item, or a door to a set of choices. */
    sealed interface Choice permits Plain, PlayerHead, StructureMaps {
    }

    record Plain(Item item) implements Choice {
    }

    record PlayerHead() implements Choice {
    }

    record StructureMaps() implements Choice {
    }

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
    protected List<Choice> entries() {
        List<Choice> choices = new ArrayList<>(List.of(new PlayerHead(), new StructureMaps()));
        CoordinateIcons.all().forEach(item -> choices.add(new Plain(item)));
        return choices;
    }

    @Override
    protected UiElement render(UiBuilder builder, Choice choice) {
        Optional<SavedCoordinate> saved = service.find(name);
        return switch (choice) {
            case Plain plain -> plain(builder, plain.item(), saved);
            case PlayerHead ignored -> head(builder, saved);
            case StructureMaps ignored -> maps(builder, saved);
        };
    }

    private UiElement plain(UiBuilder builder, Item item, Optional<SavedCoordinate> saved) {
        boolean current = saved.filter(coordinate -> coordinate.iconPlayer().isEmpty()).map(SavedCoordinate::icon)
                .filter(item::equals).isPresent();
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

    private UiElement head(UiBuilder builder, Optional<SavedCoordinate> saved) {
        Optional<PlayerRef> owner = saved.flatMap(SavedCoordinate::iconPlayer);
        ItemStack shown = owner.map(player -> PlayerHeads.item(player.id())).orElseGet(() -> new ItemStack(Items.PLAYER_HEAD));
        List<Component> lines = new ArrayList<>(List.of(factory().text(StaffMessages.Coordinates.ICON_HEAD_LORE)));
        owner.ifPresent(player -> lines.add(factory().text(
                Message.plain(StaffMessages.Coordinates.ICON_HEAD_OWNER).with("player", player.name()))));
        lines.add(Component.empty());
        lines.add(ClickHints.left(factory(), "elegir jugador"));
        return builder.button(owner.isPresent() ? shine(shown) : shown, factory().text(StaffMessages.Coordinates.ICON_HEAD_NAME),
                lines, click -> {
                    if (click.isLeft()) {
                        new CoordinateHeadMenu(ui(), service, name, this, previous()).open(click.player());
                    }
                });
    }

    private UiElement maps(UiBuilder builder, Optional<SavedCoordinate> saved) {
        boolean current = saved.filter(coordinate -> coordinate.iconPlayer().isEmpty()).map(SavedCoordinate::icon)
                .filter(CoordinateIcons.structureMaps()::contains).isPresent();
        ItemStack shown = new ItemStack(Items.ABANDONED_CAMP_MAP);
        List<Component> lines = List.of(factory().text(StaffMessages.Coordinates.ICON_MAPS_LORE), Component.empty(),
                ClickHints.left(factory(), "ver mapas"));
        return builder.button(current ? shine(shown) : shown, factory().text(StaffMessages.Coordinates.ICON_MAPS_NAME), lines,
                click -> {
                    if (click.isLeft()) {
                        new CoordinateMapMenu(ui(), service, name, this, previous()).open(click.player());
                    }
                });
    }

    private ItemStack shine(ItemStack stack) {
        return factory().item(stack).glint().build();
    }
}
