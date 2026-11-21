package com.panita.enriquecraft.chat.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.service.SudoService;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /sudo <player> <text>}: for operators. The player runs the command that starts with a slash, or says the message.
 */
@CommandSpec(name = "sudo", description = ChatMessages.Sudo.DESCRIPTION, access = PermissionLevel.ADMINS)
public final class SudoCommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";
    private static final String TEXT_ARGUMENT = "text";

    private final SudoService sudo;

    public SudoCommand(SudoService sudo) {
        this.sudo = sudo;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                .then(Commands.argument(TEXT_ARGUMENT, StringArgumentType.greedyString())
                        .executes(context -> sudo.run(context.getSource(), EntityArgument.getPlayer(context, PLAYER_ARGUMENT),
                                StringArgumentType.getString(context, TEXT_ARGUMENT)))));
    }
}
