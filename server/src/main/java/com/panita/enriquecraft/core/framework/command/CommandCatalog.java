package com.panita.enriquecraft.core.framework.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Registry of every {@link ModCommand} of the mod. Commands can be registered in any order; the
 * tree is assembled from their {@link CommandSpec#parent()} declarations when it is first read.
 */
public final class CommandCatalog {

    private final Map<Class<? extends ModCommand>, ModCommand> commands = new LinkedHashMap<>();
    private List<CommandEntry> roots;

    public void register(ModCommand command) {
        specOf(command.getClass());
        commands.put(command.getClass(), command);
        roots = null;
    }

    /**
     * The top-level commands, each with its nested commands, sorted by name.
     *
     * @throws IllegalStateException if a parent is not registered, parents form a cycle, or two
     *                               siblings share a literal
     */
    public List<CommandEntry> roots() {
        if (roots == null) {
            roots = assemble();
        }
        return roots;
    }

    /** Every command and subcommand, depth first. */
    public List<CommandEntry> entries() {
        return roots().stream().flatMap(CommandCatalog::flatten).toList();
    }

    private static Stream<CommandEntry> flatten(CommandEntry entry) {
        return Stream.concat(Stream.of(entry), entry.children().stream().flatMap(CommandCatalog::flatten));
    }

    private List<CommandEntry> assemble() {
        Map<Class<? extends ModCommand>, List<ModCommand>> childrenByParent = new LinkedHashMap<>();
        for (ModCommand command : commands.values()) {
            childrenByParent.computeIfAbsent(specOf(command.getClass()).parent(), parent -> new ArrayList<>()).add(command);
        }
        childrenByParent.keySet().stream()
                .filter(parent -> parent != ModCommand.class && !commands.containsKey(parent))
                .findFirst()
                .ifPresent(parent -> {
                    throw new IllegalStateException(parent.getName() + " is declared as a parent but is not a registered command");
                });

        List<CommandEntry> assembled = toEntries(childrenByParent.getOrDefault(ModCommand.class, List.of()), List.of(), childrenByParent);
        int reachable = (int) assembled.stream().flatMap(CommandCatalog::flatten).count();
        if (reachable != commands.size()) {
            throw new IllegalStateException("Some commands are not reachable from a top-level command, their parents form a cycle");
        }
        return assembled;
    }

    private List<CommandEntry> toEntries(List<ModCommand> siblings, List<String> parentPath,
                                         Map<Class<? extends ModCommand>, List<ModCommand>> childrenByParent) {
        Set<String> literals = new HashSet<>();
        List<CommandEntry> entries = new ArrayList<>();
        for (ModCommand command : siblings) {
            CommandSpec spec = specOf(command.getClass());
            for (String literal : Stream.concat(Stream.of(spec.name()), Stream.of(spec.aliases())).toList()) {
                if (!literals.add(literal)) {
                    throw new IllegalStateException("Duplicate command literal '" + literal + "' under /" + String.join(" ", parentPath));
                }
            }
            List<String> path = new ArrayList<>(parentPath);
            path.add(spec.name());
            List<CommandEntry> children = toEntries(childrenByParent.getOrDefault(command.getClass(), List.of()), path, childrenByParent);
            entries.add(new CommandEntry(command, spec, path, children));
        }
        entries.sort(Comparator.comparing(entry -> entry.spec().name()));
        return entries;
    }

    private static CommandSpec specOf(Class<? extends ModCommand> type) {
        CommandSpec spec = type.getAnnotation(CommandSpec.class);
        if (spec == null) {
            throw new IllegalStateException(type.getName() + " must be annotated with @CommandSpec");
        }
        return spec;
    }
}
