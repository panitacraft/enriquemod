package com.panita.enriquecraft.auth.commands.auth;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.auth.commands.WhitelistHandler;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /auth mode <player> <premium|cracked>}: changes how a whitelisted player logs in. Their UUID changes
 * with it, so what the game keeps of them is copied over to the new one.
 */
@CommandSpec(name = "mode", parent = AuthCommand.class, description = AuthMessages.Commands.MODE_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class AuthModeSubcommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";

    private final WhitelistHandler whitelist;

    public AuthModeSubcommand(WhitelistHandler whitelist) {
        this.whitelist = whitelist;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        var player = Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerList().getWhiteListNames(), suggestions));
        for (AuthMode mode : AuthMode.values()) {
            player.then(Commands.literal(mode.getSerializedName()).executes(context -> switchTo(context, mode)));
        }
        builder.then(player);
    }

    private int switchTo(CommandContext<CommandSourceStack> context, AuthMode mode) {
        return whitelist.setMode(context.getSource(), StringArgumentType.getString(context, PLAYER_ARGUMENT), mode) > 0
                ? Command.SINGLE_SUCCESS : 0;
    }
}
