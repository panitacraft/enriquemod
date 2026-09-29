package com.panita.enriquecraft.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Collection;
import java.util.function.Function;

/**
 * Reusable tab-completion providers for command arguments.
 */
public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    /**
     * Suggests values that depend on who is typing, for example only the commands they may run.
     */
    public static SuggestionProvider<CommandSourceStack> forSource(
            Function<CommandSourceStack, Collection<String>> values) {
        return (context, builder) -> SharedSuggestionProvider.suggest(values.apply(context.getSource()), builder);
    }
}
