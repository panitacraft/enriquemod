package com.panita.enriquecraft.core.framework.command;

import java.util.List;

/**
 * A registered command with its position in the command tree.
 *
 * @param command  the command instance
 * @param spec     its declaration
 * @param path     names from the top-level command down to this one, for example {@code [enriquecraft, help]}
 * @param children the commands nested directly under this one
 */
public record CommandEntry(ModCommand command, CommandSpec spec, List<String> path, List<CommandEntry> children) {

    public CommandEntry {
        path = List.copyOf(path);
        children = List.copyOf(children);
    }

    /** The path as typed by a player, for example {@code enriquecraft help}. */
    public String displayPath() {
        return String.join(" ", path);
    }

    public boolean matches(String literal) {
        return spec.name().equals(literal) || List.of(spec.aliases()).contains(literal);
    }
}
