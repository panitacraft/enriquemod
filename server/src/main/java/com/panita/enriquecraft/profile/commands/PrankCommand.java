package com.panita.enriquecraft.profile.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /prank}: the prank level of the player and the ones they may choose, each a click away from {@code /prank set}. It is the
 * way to see and change the level without the client companion's menu.
 */
@CommandSpec(name = "prank", description = ProfileMessages.Commands.PRANK_DESCRIPTION)
public final class PrankCommand implements ModCommand {

    private final PlayerOnly playerOnly;
    private final ProfileService profiles;
    private final ProfileView view;

    public PrankCommand(PlayerOnly playerOnly, ProfileService profiles, ProfileView view) {
        this.playerOnly = playerOnly;
        this.profiles = profiles;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes((context, player) -> {
            view.prankList(player, profiles.emblemsOf(player));
            return Command.SINGLE_SUCCESS;
        }));
    }
}
