package com.panita.enriquecraft.moderation.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.moderation.service.PunishmentTerms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.Locale;

/**
 * The last argument of {@code /ban} and {@code /mute}: how long, then why. It is one argument that takes the rest of the line,
 * because a reason may have any character and starts right after an optional time, so no argument type of the game could
 * tell the two apart; {@link PunishmentTerms} reads it.
 */
final class PunishmentTermsArgument {

    private static final String NAME = "time_and_reason";

    private PunishmentTermsArgument() {
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> node(Command<CommandSourceStack> executes) {
        return Commands.argument(NAME, StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
                    // Only the first word can be the time.
                    if (typed.indexOf(' ') < 0 && PunishmentTerms.PERMANENT.startsWith(typed)) {
                        builder.suggest(PunishmentTerms.PERMANENT);
                    }
                    return builder.buildFuture();
                })
                .executes(executes);
    }

    static PunishmentTerms read(CommandContext<CommandSourceStack> context) {
        return PunishmentTerms.parse(StringArgumentType.getString(context, NAME));
    }
}
