package com.panita.enriquecraft.moderation.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import com.panita.enriquecraft.moderation.service.BanService;
import com.panita.enriquecraft.moderation.service.PunishmentTerms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /ban <player> [time|permanent] [reason]}: the game's own command with a time added. {@code BanPlayerCommandsMixin} stops
 * the game from registering its version, so this is the only {@code /ban}. A ban without a time is permanent.
 */
@CommandSpec(name = "ban", description = ModerationMessages.Ban.DESCRIPTION, access = PermissionLevel.ADMINS)
public final class BanCommand implements ModCommand {

    private static final String TARGETS = "targets";

    private final BanService bans;

    public BanCommand(BanService bans) {
        this.bans = bans;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(TARGETS, GameProfileArgument.gameProfile())
                .executes(context -> bans.ban(context.getSource(), GameProfileArgument.getGameProfiles(context, TARGETS), PunishmentTerms.NONE))
                .then(PunishmentTermsArgument.node(context ->
                        bans.ban(context.getSource(), GameProfileArgument.getGameProfiles(context, TARGETS), PunishmentTermsArgument.read(context)))));
    }
}
