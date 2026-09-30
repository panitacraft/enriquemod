package com.panita.enriquecraft.core.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns {@link CommandEntry}s into Brigadier nodes. This is the only class that contains Brigadier
 * boilerplate: permission requirements, subcommand nesting and alias redirects.
 */
public final class CommandTreeBuilder {

    public void registerOn(CommandDispatcher<CommandSourceStack> dispatcher, CommandEntry entry) {
        for (LiteralCommandNode<CommandSourceStack> node : build(entry)) {
            dispatcher.getRoot().addChild(node);
        }
    }

    /**
     * Builds the node of a command followed by one redirecting node per alias.
     */
    private List<LiteralCommandNode<CommandSourceStack>> build(CommandEntry entry) {
        CommandSpec spec = entry.spec();
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(spec.name())
                .requires(CommandPermissions.requirement(entry.path(), spec.access()));
        entry.command().configure(builder);
        for (CommandEntry child : entry.children()) {
            build(child).forEach(builder::then);
        }
        LiteralCommandNode<CommandSourceStack> node = builder.build();

        List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        nodes.add(node);
        for (String alias : spec.aliases()) {
            // A redirect copies the children but not the executor or the requirement of its target.
            nodes.add(Commands.literal(alias)
                    .requires(node.getRequirement())
                    .executes(node.getCommand())
                    .redirect(node)
                    .build());
        }
        return nodes;
    }
}
