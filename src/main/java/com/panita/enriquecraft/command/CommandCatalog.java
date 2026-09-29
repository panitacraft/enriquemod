package com.panita.enriquecraft.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registry of every top-level command of the mod. It holds no commands by itself; see
 * {@link CommandModule} for the list.
 */
public final class CommandCatalog {

    private final List<ModCommand> commands = new ArrayList<>();
    private final Set<String> usedLiterals = new HashSet<>();

    public void register(ModCommand command) {
        CommandMetadata metadata = command.metadata();
        List<String> literals = new ArrayList<>(metadata.aliases());
        literals.add(metadata.name());
        for (String literal : literals) {
            if (!usedLiterals.add(literal)) {
                throw new IllegalStateException("Duplicate top-level command literal: " + literal);
            }
        }
        commands.add(command);
    }

    public List<ModCommand> commands() {
        return List.copyOf(commands);
    }

    /**
     * Lists every command and subcommand, depth first, each with its full path.
     */
    public List<CommandEntry> entries() {
        List<CommandEntry> entries = new ArrayList<>();
        for (ModCommand command : commands) {
            collect(List.of(), command, entries);
        }
        return entries;
    }

    private void collect(List<String> parentPath, ModCommand command, List<CommandEntry> entries) {
        List<String> path = new ArrayList<>(parentPath);
        path.add(command.metadata().name());
        entries.add(new CommandEntry(path, command));
        for (ModCommand subcommand : command.subcommands()) {
            collect(path, subcommand, entries);
        }
    }
}
