package com.panita.enriquecraft.profile.commands.profile;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.profile.commands.ProfileCommand;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /profile refresh}: the player reads their own emblems again from Panitacraft, after a wait that keeps the command from
 * being used to flood the API. {@code /profile refresh all} is for administrators.
 */
@CommandSpec(name = "refresh", parent = ProfileCommand.class, description = ProfileMessages.Commands.REFRESH_DESCRIPTION)
public final class RefreshSubcommand implements ModCommand {

    private final ProfileRefresher refresher;
    private final ProfileView view;
    private final PlayerOnly playerOnly;

    public RefreshSubcommand(ProfileRefresher refresher, ProfileView view, PlayerOnly playerOnly) {
        this.refresher = refresher;
        this.view = view;
        this.playerOnly = playerOnly;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes((context, player) -> {
            refresher.refreshSelf(player).thenAccept(result -> view.selfRefresh(player, result));
            return Command.SINGLE_SUCCESS;
        }));
    }
}
