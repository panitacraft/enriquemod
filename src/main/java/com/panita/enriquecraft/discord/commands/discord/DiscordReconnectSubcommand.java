package com.panita.enriquecraft.discord.commands.discord;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.message.DiscordView;
import com.panita.enriquecraft.discord.service.BridgeConnection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /discord reconnect}: drops the line to the bot and connects again now, which also picks up a new address or key.
 */
@CommandSpec(name = "reconnect", parent = DiscordCommand.class, description = DiscordMessages.Commands.RECONNECT_DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class DiscordReconnectSubcommand implements ModCommand {

    private final BridgeConnection connection;
    private final DiscordView view;

    public DiscordReconnectSubcommand(BridgeConnection connection, DiscordView view) {
        this.connection = connection;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::reconnect);
    }

    private int reconnect(CommandContext<CommandSourceStack> context) {
        view.reconnecting(context.getSource());
        connection.reconnect();
        return Command.SINGLE_SUCCESS;
    }
}
