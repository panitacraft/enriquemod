package com.panita.enriquecraft.profile.commands.profile;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.profile.commands.ProfileCommand;
import com.panita.enriquecraft.profile.gui.ProfileMenus;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import net.minecraft.commands.CommandSourceStack;

/** {@code /profile edit}: opens the player's own profile ready to be edited. */
@CommandSpec(name = "edit", parent = ProfileCommand.class, description = ProfileMessages.Commands.EDIT_DESCRIPTION)
public final class EditSubcommand implements ModCommand {

    private final ProfileMenus menus;
    private final PlayerOnly playerOnly;

    public EditSubcommand(ProfileMenus menus, PlayerOnly playerOnly) {
        this.menus = menus;
        this.playerOnly = playerOnly;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes((context, player) -> {
            menus.edit(player);
            return Command.SINGLE_SUCCESS;
        }));
    }
}
