package com.panita.enriquecraft.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /broadcast <prefixed|raw> <message>}: sends a chat message to every player and the
 * console, with the mod prefix ({@code prefixed}) or exactly as written ({@code raw}). The text
 * accepts text tags and legacy color codes.
 */
@CommandSpec(name = "broadcast", description = Messages.Broadcast.DESCRIPTION, access = PermissionLevel.ADMINS)
public final class BroadcastCommand implements ModCommand {

    private static final String PREFIXED = "prefixed";
    private static final String RAW = "raw";
    private static final String MESSAGE_ARGUMENT = "message";

    private final Messenger messenger;

    public BroadcastCommand(Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(mode(PREFIXED, true)).then(mode(RAW, false));
    }

    private ArgumentBuilder<CommandSourceStack, ?> mode(String literal, boolean prefixed) {
        return Commands.literal(literal)
                .then(Commands.argument(MESSAGE_ARGUMENT, StringArgumentType.greedyString())
                        .executes(context -> execute(context, prefixed)));
    }

    private int execute(CommandContext<CommandSourceStack> context, boolean prefixed) {
        String text = StringArgumentType.getString(context, MESSAGE_ARGUMENT);
        MinecraftServer server = context.getSource().getServer();
        if (prefixed) {
            messenger.prefixedBroadcast(server, text);
        } else {
            messenger.broadcast(server, text);
        }
        return Command.SINGLE_SUCCESS;
    }
}
