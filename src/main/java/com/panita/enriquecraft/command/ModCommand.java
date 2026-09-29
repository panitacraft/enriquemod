package com.panita.enriquecraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

/**
 * A command of the mod. Implementations only describe the command; permissions, aliases, nesting
 * and registration are handled by the framework.
 * <p>
 * A command with no parent becomes a top-level command. A command returned by
 * {@link #subcommands()} is nested under its parent.
 */
public interface ModCommand {

    CommandMetadata metadata();

    /**
     * Adds arguments and executors to the command's node.
     *
     * @param builder the node builder for this command; permission and aliases are already handled
     */
    void configure(LiteralArgumentBuilder<CommandSourceStack> builder);

    default List<ModCommand> subcommands() {
        return List.of();
    }
}
