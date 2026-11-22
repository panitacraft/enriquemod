package com.panita.enriquecraft.profile.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandPermissions;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.profile.gui.ProfileMenus;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;

/**
 * {@code /profile} (also {@code /perfil}): opens your own profile. Another player's is {@code /profile view <player>}. The console
 * gets the list of what the command can do.
 */
@CommandSpec(name = "profile", aliases = "perfil", description = ProfileMessages.Commands.PROFILE_DESCRIPTION, clickable = true)
public final class ProfileCommand implements ModCommand {

    private static final String NAME = "profile";
    private static final List<String> PATH = List.of(NAME);

    private final HelpView helpView;
    private final ProfileMenus menus;
    private final ProfileView view;

    public ProfileCommand(HelpView helpView, ProfileMenus menus, ProfileView view) {
        this.helpView = helpView;
        this.menus = menus;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                helpView.sendUsage(context.getSource(), NAME);
            } else if (allowed(context.getSource(), player)) {
                menus.open(player);
            }
            return Command.SINGLE_SUCCESS;
        });
    }

    /** A clickable command is always in the tree, so the permission is checked here, as the game would have. */
    private boolean allowed(CommandSourceStack source, ServerPlayer player) {
        if (CommandPermissions.canUse(source, PATH, PermissionLevel.ALL)) {
            return true;
        }
        view.notAllowed(player);
        return false;
    }
}
