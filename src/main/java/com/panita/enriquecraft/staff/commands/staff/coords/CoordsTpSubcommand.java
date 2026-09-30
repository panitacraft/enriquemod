package com.panita.enriquecraft.staff.commands.staff.coords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Optional;

/**
 * {@code /staff coords tp <name>}: teleports the staff member to a saved coordinate.
 */
@CommandSpec(name = "tp", parent = CoordsSubcommand.class, description = StaffMessages.Coordinates.TP_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class CoordsTpSubcommand implements ModCommand {

    private static final String NAME_ARGUMENT = "name";

    private final PlayerOnly playerOnly;
    private final CoordinateService service;
    private final CoordinateView view;

    public CoordsTpSubcommand(PlayerOnly playerOnly, CoordinateService service, CoordinateView view) {
        this.playerOnly = playerOnly;
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                .suggests(CommandSuggestions.forSource(source -> service.names()))
                .executes(playerOnly.executes(this::teleport)));
    }

    private int teleport(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        Optional<SavedCoordinate> coordinate = service.find(name);
        if (coordinate.isEmpty()) {
            view.notFound(context.getSource(), name);
            return 0;
        }
        view.teleport(player, coordinate.get());
        return Command.SINGLE_SUCCESS;
    }
}
