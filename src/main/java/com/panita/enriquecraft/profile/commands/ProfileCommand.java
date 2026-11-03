package com.panita.enriquecraft.profile.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /profile} (also {@code /perfil}): root of what a player can do with their profile. Running it alone lists the commands.
 */
@CommandSpec(name = "profile", aliases = "perfil", description = ProfileMessages.Commands.PROFILE_DESCRIPTION)
public final class ProfileCommand implements ModCommand {

    private static final String NAME = "profile";

    private final HelpView helpView;

    public ProfileCommand(HelpView helpView) {
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
