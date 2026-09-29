package com.panita.enriquecraft.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.command.CommandMetadata;
import com.panita.enriquecraft.command.ModCommand;
import com.panita.enriquecraft.message.Message;
import com.panita.enriquecraft.message.Messages;
import com.panita.enriquecraft.message.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /ping}: shows the latency of the player who runs it.
 */
public final class PingCommand implements ModCommand {

    private final Messenger messenger;

    public PingCommand(Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public CommandMetadata metadata() {
        return CommandMetadata.of("ping", Messages.Ping.DESCRIPTION, PermissionLevel.ALL);
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            messenger.send(source, Message.error(Messages.Ping.PLAYERS_ONLY).prefixed());
            return 0;
        }
        messenger.send(player, Message.info(Messages.Ping.RESULT).prefixed().with("ping", player.connection.latency()));
        return Command.SINGLE_SUCCESS;
    }
}
