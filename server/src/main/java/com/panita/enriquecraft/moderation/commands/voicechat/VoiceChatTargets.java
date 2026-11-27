package com.panita.enriquecraft.moderation.commands.voicechat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/** The two ways {@code mute} and {@code unmute} are given a target: {@code all}, or the name of a player. */
final class VoiceChatTargets {

    private static final String ALL = "all";
    private static final String PLAYER = "player";

    private VoiceChatTargets() {
    }

    static void attach(LiteralArgumentBuilder<CommandSourceStack> builder, ToIntFunction<CommandSourceStack> everyone,
                       BiFunction<CommandSourceStack, String, Integer> one) {
        builder.then(Commands.literal(ALL).executes(context -> everyone.applyAsInt(context.getSource())))
                .then(Commands.argument(PLAYER, StringArgumentType.word())
                        .suggests((context, suggestions) ->
                                SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getPlayerNamesArray(), suggestions))
                        .executes(context -> one.apply(context.getSource(), StringArgumentType.getString(context, PLAYER))));
    }
}
