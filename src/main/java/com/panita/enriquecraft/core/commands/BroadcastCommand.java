package com.panita.enriquecraft.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /broadcast <message>}: sends a prefixed chat announcement to every player and the console.
 */
@CommandSpec(name = "broadcast", description = Messages.Broadcast.DESCRIPTION, access = PermissionLevel.ADMINS)
public final class BroadcastCommand implements ModCommand {

    private static final String MESSAGE_ARGUMENT = "message";

    private final Messenger messenger;

    public BroadcastCommand(Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(MESSAGE_ARGUMENT, StringArgumentType.greedyString())
                .executes(this::execute));
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        String text = StringArgumentType.getString(context, MESSAGE_ARGUMENT);
        messenger.prefixedBroadcast(context.getSource().getServer(), text);
        return Command.SINGLE_SUCCESS;
    }
}
