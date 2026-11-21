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
 * {@code /discord status}: whether the server is connected to the bot, the channels it bridges and how the traffic goes.
 */
@CommandSpec(name = "status", parent = DiscordCommand.class, description = DiscordMessages.Commands.STATUS_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class DiscordStatusSubcommand implements ModCommand {

    private final BridgeConnection connection;
    private final DiscordView view;

    public DiscordStatusSubcommand(BridgeConnection connection, DiscordView view) {
        this.connection = connection;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::status);
    }

    private int status(CommandContext<CommandSourceStack> context) {
        view.status(context.getSource(), connection.status());
        return Command.SINGLE_SUCCESS;
    }
}
