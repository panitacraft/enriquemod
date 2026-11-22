package com.panita.enriquecraft.profile.commands.profile;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.chat.service.KnownPlayers;
import com.panita.enriquecraft.core.framework.command.CommandPermissions;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.profile.commands.ProfileCommand;
import com.panita.enriquecraft.profile.gui.ProfileMenus;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;

/**
 * {@code /profile view <player>}: opens the profile of a player who is online or on the whitelist. It is {@code clickable}, since a
 * click on a head in the chat runs it, so its executor checks the permission itself.
 */
@CommandSpec(name = "view", parent = ProfileCommand.class, description = ProfileMessages.Commands.VIEW_DESCRIPTION, clickable = true)
public final class ViewSubcommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";
    private static final List<String> PATH = List.of("profile", "view");

    private final HelpView helpView;
    private final ProfileMenus menus;
    private final KnownPlayers known;
    private final ProfileView view;

    public ViewSubcommand(HelpView helpView, ProfileMenus menus, KnownPlayers known, ProfileView view) {
        this.helpView = helpView;
        this.menus = menus;
        this.known = known;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(known.namesOn(context.getSource().getServer()), suggestions))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        helpView.sendUsage(context.getSource(), PATH.get(0));
                        return 0;
                    }
                    if (!CommandPermissions.canUse(context.getSource(), PATH, PermissionLevel.ALL)) {
                        view.notAllowed(player);
                        return 0;
                    }
                    menus.openOf(player, StringArgumentType.getString(context, PLAYER_ARGUMENT));
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
