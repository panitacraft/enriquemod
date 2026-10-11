package com.panita.enriquecraft.auth.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.message.AdminView;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /logout}: ends the remembered login of a player who uses a PIN and disconnects them, so the PIN is
 * asked for the next time they join.
 */
@CommandSpec(name = "logout", description = AuthMessages.Commands.LOGOUT_DESCRIPTION)
public final class LogoutCommand implements ModCommand {

    private final PlayerOnly playerOnly;
    private final AccountService accounts;
    private final RefusalView refusals;
    private final AdminView view;

    public LogoutCommand(PlayerOnly playerOnly, AccountService accounts, RefusalView refusals, AdminView view) {
        this.playerOnly = playerOnly;
        this.accounts = accounts;
        this.refusals = refusals;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes(this::logout));
    }

    private int logout(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (!context.getSource().getServer().isDedicatedServer()) {
            view.notAServer(context.getSource());
            return 0;
        }
        if (AuthMode.of(player.getUUID(), player.getGameProfile().name()) == AuthMode.PREMIUM) {
            view.premiumHasNoSession(context.getSource());
            return 0;
        }
        accounts.endSession(player.getUUID());
        player.connection.disconnect(refusals.loggedOut());
        return Command.SINGLE_SUCCESS;
    }
}
