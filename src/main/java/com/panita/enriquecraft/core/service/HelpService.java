package com.panita.enriquecraft.core.service;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.panita.enriquecraft.core.framework.command.CommandCatalog;
import com.panita.enriquecraft.core.framework.command.CommandEntry;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.Optional;

/**
 * Decides which commands a sender may see and how they are used. Visibility is read from the
 * registered command tree, so it always matches what the sender can actually run.
 */
public final class HelpService {

    private final CommandCatalog catalog;

    public HelpService(CommandCatalog catalog) {
        this.catalog = catalog;
    }

    /** Every command and subcommand the sender may run, depth first. */
    public List<CommandEntry> visibleEntries(CommandSourceStack source) {
        return catalog.entries().stream()
                .filter(entry -> findNode(source, entry.path()) != null)
                .toList();
    }

    /** Names of the top-level commands the sender may run. */
    public List<String> visibleNames(CommandSourceStack source) {
        return catalog.commands().stream()
                .filter(command -> findNode(source, List.of(command.metadata().name())) != null)
                .map(command -> command.metadata().name())
                .toList();
    }

    /** Finds a visible top-level command by its name or one of its aliases. */
    public Optional<CommandEntry> findTopLevel(CommandSourceStack source, String literal) {
        return catalog.commands().stream()
                .filter(command -> matches(command, literal))
                .map(command -> new CommandEntry(List.of(command.metadata().name()), command))
                .filter(entry -> findNode(source, entry.path()) != null)
                .findFirst();
    }

    /**
     * Every executable usage of a command, derived from its registered tree, for example
     * {@code enriquecraft help <command>}.
     */
    public List<String> usages(CommandSourceStack source, CommandEntry entry) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(source);
        CommandNode<CommandSourceStack> node = findNode(source, entry.path());
        if (node == null) {
            return List.of();
        }
        return List.of(dispatcher.getAllUsage(node, source, true)).stream()
                .map(relative -> relative.isEmpty() ? entry.displayPath() : entry.displayPath() + " " + relative)
                .toList();
    }

    private boolean matches(ModCommand command, String literal) {
        return command.metadata().name().equals(literal) || command.metadata().aliases().contains(literal);
    }

    /** Returns the node at the path if the sender can use it and every parent above it, otherwise null. */
    private CommandNode<CommandSourceStack> findNode(CommandSourceStack source, List<String> path) {
        CommandNode<CommandSourceStack> node = dispatcher(source).getRoot();
        for (String literal : path) {
            node = node.getChild(literal);
            if (node == null || !node.canUse(source)) {
                return null;
            }
        }
        return node;
    }

    private CommandDispatcher<CommandSourceStack> dispatcher(CommandSourceStack source) {
        return source.getServer().getCommands().getDispatcher();
    }
}
