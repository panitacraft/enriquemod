package com.panita.enriquecraft.profile.commands.profile.refresh;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.profile.commands.profile.RefreshSubcommand;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /profile refresh all}: reads again the emblems of every player online, for a change that cannot wait for each player to
 * join again or open their menu.
 */
@CommandSpec(name = "all", parent = RefreshSubcommand.class, description = ProfileMessages.Commands.REFRESH_ALL_DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class RefreshAllSubcommand implements ModCommand {

    private final ProfileRefresher refresher;
    private final ProfileView view;

    public RefreshAllSubcommand(ProfileRefresher refresher, ProfileView view) {
        this.refresher = refresher;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            CommandSourceStack source = context.getSource();
            refresher.refreshAll(source.getServer()).thenAccept(report -> view.allRefreshed(source, report));
            return Command.SINGLE_SUCCESS;
        });
    }
}
