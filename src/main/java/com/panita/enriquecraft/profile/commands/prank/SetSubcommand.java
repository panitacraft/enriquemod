package com.panita.enriquecraft.profile.commands.prank;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.profile.ProfilePermissions;
import com.panita.enriquecraft.profile.commands.PrankCommand;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /prank set <level>}: changes the prank level of the player, under the same rules as the menu: the starting level cannot be
 * chosen, and a change must wait for the one before it. The ids are completed from the levels the server has.
 */
@CommandSpec(name = "set", parent = PrankCommand.class, description = ProfileMessages.Commands.PRANK_SET_DESCRIPTION)
public final class SetSubcommand implements ModCommand {

    private static final String LEVEL_ARGUMENT = "level";

    private final PlayerOnly playerOnly;
    private final ProfileService profiles;
    private final ProfileView view;

    public SetSubcommand(PlayerOnly playerOnly, ProfileService profiles, ProfileView view) {
        this.playerOnly = playerOnly;
        this.profiles = profiles;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(LEVEL_ARGUMENT, StringArgumentType.word())
                .suggests(CommandSuggestions.forSource(this::choosable))
                .executes(playerOnly.executes(this::set)));
    }

    private int set(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String id = StringArgumentType.getString(context, LEVEL_ARGUMENT);
        String title = profiles.prankLevels().stream().filter(level -> level.id().equals(id)).findFirst().map(EmblemView::title).orElse(id);
        ProfileService.PrankChange change = profiles.choosePrank(player, id, ProfilePermissions.canBypassPrankCooldown(player));
        view.prankResult(player, title, change);
        return change.status() == ProfileService.PrankChange.Status.CHANGED ? Command.SINGLE_SUCCESS : 0;
    }

    /** The levels that can be chosen: all but the first, which is only where players start. */
    private Collection<String> choosable(CommandSourceStack source) {
        return profiles.prankLevels().stream().skip(1).map(EmblemView::id).toList();
    }
}
