package com.panita.enriquecraft.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /ping}: shows the latency of the player who runs it.
 */
@CommandSpec(name = "ping", description = Messages.Ping.DESCRIPTION)
public final class PingCommand implements ModCommand {

    private final Messenger messenger;
    private final PlayerOnly playerOnly;

    public PingCommand(Messenger messenger, PlayerOnly playerOnly) {
        this.messenger = messenger;
        this.playerOnly = playerOnly;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes(this::execute));
    }

    private int execute(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        messenger.send(player, Message.info(Messages.Ping.RESULT).prefixed().with("ping", player.connection.latency()));
        return Command.SINGLE_SUCCESS;
    }
}
