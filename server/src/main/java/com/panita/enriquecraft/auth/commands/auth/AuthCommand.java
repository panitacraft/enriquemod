package com.panita.enriquecraft.auth.commands.auth;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /auth}: root of the tools staff use to manage how players log in. Running it alone lists them.
 */
@CommandSpec(name = "auth", description = AuthMessages.Commands.AUTH_DESCRIPTION, access = PermissionLevel.GAMEMASTERS)
public final class AuthCommand implements ModCommand {

    private static final String NAME = "auth";

    private final HelpView helpView;

    public AuthCommand(HelpView helpView) {
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
