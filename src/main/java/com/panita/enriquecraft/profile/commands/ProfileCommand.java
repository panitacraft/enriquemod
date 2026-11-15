package com.panita.enriquecraft.profile.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.chat.service.KnownPlayers;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.profile.gui.ProfileMenus;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /profile} (also {@code /perfil}): a player's profile. Running it opens your own; with a player's name it opens theirs. The
 * console gets the list of what the command can do.
 */
@CommandSpec(name = "profile", aliases = "perfil", description = ProfileMessages.Commands.PROFILE_DESCRIPTION)
public final class ProfileCommand implements ModCommand {

    private static final String NAME = "profile";
    private static final String PLAYER_ARGUMENT = "player";

    private final HelpView helpView;
    private final ProfileMenus menus;
    private final KnownPlayers known;

    public ProfileCommand(HelpView helpView, ProfileMenus menus, KnownPlayers known) {
        this.helpView = helpView;
        this.menus = menus;
        this.known = known;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                menus.open(player);
            } else {
                helpView.sendUsage(context.getSource(), NAME);
            }
            return Command.SINGLE_SUCCESS;
        });
        builder.then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(known.namesOn(context.getSource().getServer()), suggestions))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        helpView.sendUsage(context.getSource(), NAME);
                        return 0;
                    }
                    menus.openOf(player, StringArgumentType.getString(context, PLAYER_ARGUMENT));
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
