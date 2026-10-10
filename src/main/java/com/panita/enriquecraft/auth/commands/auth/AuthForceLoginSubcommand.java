package com.panita.enriquecraft.auth.commands.auth;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.panita.enriquecraft.auth.message.AdminView;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /auth forcelogin <player>}: lets a player who is waiting at the PIN screen in, this time only and
 * without remembering the login.
 */
@CommandSpec(name = "forcelogin", parent = AuthCommand.class, description = AuthMessages.Commands.FORCE_LOGIN_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class AuthForceLoginSubcommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";

    private final HeldPlayers held;
    private final AdminView view;

    public AuthForceLoginSubcommand(HeldPlayers held, AdminView view) {
        this.held = held;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player()).executes(this::force));
    }

    private int force(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        String name = player.getGameProfile().name();
        if (!held.forceLogin(player)) {
            view.notWaiting(context.getSource(), name);
            return 0;
        }
        view.forced(context.getSource(), name);
        return Command.SINGLE_SUCCESS;
    }
}
