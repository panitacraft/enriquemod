package com.panita.enriquecraft.discord.commands.discord;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /discord}: root of the tools for the connection with the Panita Bot. Running it alone lists them.
 */
@CommandSpec(name = "discord", description = DiscordMessages.Commands.DISCORD_DESCRIPTION, access = PermissionLevel.GAMEMASTERS)
public final class DiscordCommand implements ModCommand {

    private static final String NAME = "discord";

    private final HelpView helpView;

    public DiscordCommand(HelpView helpView) {
        this.helpView = helpView;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            helpView.sendUsage(context.getSource(), NAME);
            return Command.SINGLE_SUCCESS;
        });
    }
}
