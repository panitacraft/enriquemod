package com.panita.enriquecraft.auth.commands.auth;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.auth.data.AuthAccount;
import com.panita.enriquecraft.auth.message.AdminView;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Optional;

/**
 * {@code /auth reset <player>}: forgets a player's PIN and remembered login, for a player who forgot their
 * PIN. They choose a new one the next time they join.
 */
@CommandSpec(name = "reset", parent = AuthCommand.class, description = AuthMessages.Commands.RESET_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class AuthResetSubcommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";

    private final AccountService accounts;
    private final AdminView view;

    public AuthResetSubcommand(AccountService accounts, AdminView view) {
        this.accounts = accounts;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(accounts.names(), suggestions))
                .executes(this::reset));
    }

    private int reset(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        Optional<AuthAccount> account = accounts.findByName(name);
        if (account.isEmpty() || account.get().pin().isEmpty()) {
            view.noAccount(context.getSource(), name);
            return 0;
        }
        accounts.reset(account.get().id());
        view.resetDone(context.getSource(), account.get().name());
        return Command.SINGLE_SUCCESS;
    }
}
