package com.panita.enriquecraft.moderation.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import com.panita.enriquecraft.moderation.service.MuteService;
import com.panita.enriquecraft.moderation.service.PunishmentTerms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /mute <player> [time|permanent] [reason]}: the player cannot write in the chat, send private messages or use
 * {@code /me}, except in the staff channel. A mute without a time is permanent.
 */
@CommandSpec(name = "mute", description = ModerationMessages.Mute.DESCRIPTION, access = PermissionLevel.ADMINS)
public final class MuteCommand implements ModCommand {

    private static final String TARGETS = "targets";

    private final MuteService mutes;

    public MuteCommand(MuteService mutes) {
        this.mutes = mutes;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(TARGETS, GameProfileArgument.gameProfile())
                .executes(context -> mutes.mute(context.getSource(), GameProfileArgument.getGameProfiles(context, TARGETS), PunishmentTerms.NONE))
                .then(PunishmentTermsArgument.node(context ->
                        mutes.mute(context.getSource(), GameProfileArgument.getGameProfiles(context, TARGETS), PunishmentTermsArgument.read(context)))));
    }
}
