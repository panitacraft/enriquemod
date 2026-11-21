package com.panita.enriquecraft.staff.gui;

import com.mojang.brigadier.tree.CommandNode;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.CustomItemService;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;

/**
 * Opens the staff menu and the menus it leads to. It holds what those menus need, so the command and the
 * client's key can open the menu without knowing about any of them.
 */
public final class StaffMenus {

    private final UiService ui;
    private final CoordinateService coordinates;
    private final CoordinateView coordinateView;
    private final CustomItemService customItems;
    private final CustomItemView customItemView;
    private final DeathInventoryService deaths;
    private final DeathInventoryView deathView;

    public StaffMenus(UiService ui, CoordinateService coordinates, CoordinateView coordinateView,
                      CustomItemService customItems, CustomItemView customItemView, DeathInventoryService deaths,
                      DeathInventoryView deathView) {
        this.ui = ui;
        this.coordinates = coordinates;
        this.coordinateView = coordinateView;
        this.customItems = customItems;
        this.customItemView = customItemView;
        this.deaths = deaths;
        this.deathView = deathView;
    }

    /** Opens the staff menu with the tools the player may use. */
    public void open(ServerPlayer player) {
        new StaffMenu(ui, this, allowedFor(player)).open(player);
    }

    /** The menu of a section, with the staff menu to go back to. */
    UiMenu menuOf(StaffSection section, UiMenu staffMenu) {
        return switch (section) {
            case COORDINATES -> new CoordinatesMenu(ui, coordinates, coordinateView, staffMenu);
            case ITEMS -> new CustomItemsMenu(ui, customItems, customItemView, staffMenu);
            case DEATHS -> new DeathPlayersMenu(ui, deaths, deathView, staffMenu);
        };
    }

    private static Set<StaffSection> allowedFor(ServerPlayer player) {
        CommandSourceStack source = player.createCommandSourceStack();
        var dispatcher = player.level().getServer().getCommands().getDispatcher();
        Set<StaffSection> allowed = EnumSet.noneOf(StaffSection.class);
        for (StaffSection section : StaffSection.values()) {
            CommandNode<CommandSourceStack> node = dispatcher.findNode(section.command());
            if (node != null && node.canUse(source)) {
                allowed.add(section);
            }
        }
        return allowed;
    }
}
