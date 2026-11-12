package com.panita.enriquecraft.profile.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.profile.gui.ProfileMenus;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /profile} (also {@code /perfil}): a player's own profile. Running it opens the menu; the console gets the list of what the
 * command can do.
 */
@CommandSpec(name = "profile", aliases = "perfil", description = ProfileMessages.Commands.PROFILE_DESCRIPTION)
public final class ProfileCommand implements ModCommand {

    private static final String NAME = "profile";

    private final HelpView helpView;
    private final ProfileMenus menus;

    public ProfileCommand(HelpView helpView, ProfileMenus menus) {
        this.helpView = helpView;
        this.menus = menus;
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
    }
}
