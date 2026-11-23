package com.panita.enriquecraft.core.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Registers the command and its aliases after removing whatever the root already has under those literals, so nothing another mod
     * put there (its executor, its subcommands, its requirement) is left in the way. The command that stood under the name of the entry
     * stays reachable as {@link CommandSpec#displacedAs()}, when the entry says so.
     */
    public void replaceOn(CommandDispatcher<CommandSourceStack> dispatcher, CommandEntry entry) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        for (LiteralCommandNode<CommandSourceStack> node : build(entry)) {
            CommandNode<CommandSourceStack> displaced = root.getChild(node.getName());
            String keptAs = entry.spec().displacedAs();
            if (displaced != null && !keptAs.isEmpty() && node.getName().equals(entry.spec().name())) {
                // A redirect keeps the displaced node whole (its executor and subcommands) without it standing under the old name
                root.addChild(Commands.literal(keptAs).requires(displaced.getRequirement()).executes(displaced.getCommand())
                        .redirect(displaced).build());
            }
            // Brigadier has no way to remove a node; parsing also looks in the table of literals, so both tables are cleared.
            childTable(root, "children").remove(node.getName());
            childTable(root, "literals").remove(node.getName());
            root.addChild(node);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> childTable(CommandNode<CommandSourceStack> node, String field) {
        try {
            Field table = CommandNode.class.getDeclaredField(field);
            table.setAccessible(true);
            return (Map<String, ?>) table.get(node);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Brigadier no longer has the table '" + field + "' of command nodes", exception);
        }
    }

    /**
     * Builds the node of a command followed by one redirecting node per alias.
     */
    private List<LiteralCommandNode<CommandSourceStack>> build(CommandEntry entry) {
        CommandSpec spec = entry.spec();
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(spec.name())
                .requires(spec.clickable() ? source -> true : CommandPermissions.requirement(entry.path(), spec.access()));
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
