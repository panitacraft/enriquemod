package com.panita.enriquecraft.command;

import java.util.List;

/**
 * A command together with its full path, for example {@code [enriquecraft, help]}.
 */
public record CommandEntry(List<String> path, ModCommand command) {

    public CommandEntry {
        path = List.copyOf(path);
    }

    /** The path as typed by a player, for example {@code enriquecraft help}. */
    public String displayPath() {
        return String.join(" ", path);
    }
}
