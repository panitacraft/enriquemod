package com.panita.enriquecraft.moderation.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import com.panita.enriquecraft.moderation.service.MuteService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /unmute <player>}: the player can write again. It asks for the name the player was muted under and suggests the
 * muted ones, so it works for a player who is not online.
 */
@CommandSpec(name = "unmute", description = ModerationMessages.Mute.UNMUTE_DESCRIPTION, access = PermissionLevel.ADMINS)
public final class UnmuteCommand implements ModCommand {

    private static final String PLAYER = "player";

    private final MuteService mutes;

    public UnmuteCommand(MuteService mutes) {
        this.mutes = mutes;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(PLAYER, StringArgumentType.word())
                .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(mutes.mutedNames(), suggestions))
                .executes(context -> mutes.unmute(context.getSource(), StringArgumentType.getString(context, PLAYER))));
    }
}
