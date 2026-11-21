package com.panita.enriquecraft.core.framework.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/**
 * A command of the mod. Implementations must be annotated with {@link CommandSpec}, declare a
 * single public constructor whose parameters are registered services, and only add arguments and
 * executors; permissions, aliases, nesting and registration are handled by the framework.
 */
public interface ModCommand {

    /**
     * Adds arguments and executors to the command's node.
     *
     * @param builder the node builder for this command; permission and aliases are already handled
     */
    void configure(LiteralArgumentBuilder<CommandSourceStack> builder);
}
