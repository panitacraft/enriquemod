package com.panita.enriquecraft.staff.commands.staff.coords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.staff.commands.staff.StaffCommand;
import com.panita.enriquecraft.staff.gui.CoordinatesMenu;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /staff coords}: opens the menu of saved coordinates. The subcommands save, remove and
 * teleport to them.
 */
@CommandSpec(name = "coords", parent = StaffCommand.class, description = StaffMessages.Coordinates.DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class CoordsSubcommand implements ModCommand {

    private final PlayerOnly playerOnly;
    private final MenuFactory menuFactory;
    private final CoordinateService service;
    private final CoordinateView view;

    public CoordsSubcommand(PlayerOnly playerOnly, MenuFactory menuFactory, CoordinateService service,
                            CoordinateView view) {
        this.playerOnly = playerOnly;
        this.menuFactory = menuFactory;
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes(this::openMenu));
    }

    private int openMenu(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        new CoordinatesMenu(menuFactory, service, view).open(player);
        return Command.SINGLE_SUCCESS;
    }
}
