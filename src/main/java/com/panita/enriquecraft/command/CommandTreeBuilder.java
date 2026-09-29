package com.panita.enriquecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns {@link ModCommand}s into Brigadier nodes. This is the only class that contains Brigadier
 * boilerplate: permission requirements, subcommand nesting and alias redirects.
 */
public final class CommandTreeBuilder {

    public void registerOn(CommandDispatcher<CommandSourceStack> dispatcher, ModCommand command) {
        for (LiteralCommandNode<CommandSourceStack> node : build(command, List.of())) {
            dispatcher.getRoot().addChild(node);
        }
    }

    /**
     * Builds the node of a command followed by one redirecting node per alias.
     */
    private List<LiteralCommandNode<CommandSourceStack>> build(ModCommand command, List<String> parentPath) {
        CommandMetadata metadata = command.metadata();
        List<String> path = new ArrayList<>(parentPath);
        path.add(metadata.name());

        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(metadata.name())
                .requires(CommandPermissions.requirement(path, metadata.permission()));
        command.configure(builder);
        for (ModCommand subcommand : command.subcommands()) {
            build(subcommand, path).forEach(builder::then);
        }
        LiteralCommandNode<CommandSourceStack> node = builder.build();

        List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        nodes.add(node);
        for (String alias : metadata.aliases()) {
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
