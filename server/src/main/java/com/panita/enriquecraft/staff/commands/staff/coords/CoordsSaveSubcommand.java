package com.panita.enriquecraft.staff.commands.staff.coords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateIcons;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code /staff coords save <name>}: saves the position of the staff member with a random icon.
 */
@CommandSpec(name = "save", parent = CoordsSubcommand.class, description = StaffMessages.Coordinates.SAVE_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class CoordsSaveSubcommand implements ModCommand {

    private static final String NAME_ARGUMENT = "name";

    private final PlayerOnly playerOnly;
    private final CoordinateService service;
    private final CoordinateView view;

    public CoordsSaveSubcommand(PlayerOnly playerOnly, CoordinateService service, CoordinateView view) {
        this.playerOnly = playerOnly;
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                .executes(playerOnly.executes(this::save)));
    }

    private int save(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        SavedCoordinate coordinate = SavedCoordinate.at(player, name, CoordinateIcons.random(ThreadLocalRandom.current()));
        CoordinateService.AddResult result = service.add(coordinate);
        if (result != CoordinateService.AddResult.ADDED) {
            view.rejected(context.getSource(), name, result);
            return 0;
        }
        view.saved(context.getSource(), coordinate);
        return Command.SINGLE_SUCCESS;
    }
}
