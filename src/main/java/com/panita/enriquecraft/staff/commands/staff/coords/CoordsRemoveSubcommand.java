package com.panita.enriquecraft.staff.commands.staff.coords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /staff coords remove <name>}: deletes a saved coordinate.
 */
@CommandSpec(name = "remove", parent = CoordsSubcommand.class, description = StaffMessages.Coordinates.REMOVE_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class CoordsRemoveSubcommand implements ModCommand {

    private static final String NAME_ARGUMENT = "name";

    private final CoordinateService service;
    private final CoordinateView view;

    public CoordsRemoveSubcommand(CoordinateService service, CoordinateView view) {
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                .suggests(CommandSuggestions.forSource(source -> service.names()))
                .executes(this::remove));
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        if (!service.remove(name)) {
            view.notFound(context.getSource(), name);
            return 0;
        }
        view.removed(context.getSource(), name);
        return Command.SINGLE_SUCCESS;
    }
}
