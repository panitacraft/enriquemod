package com.panita.enriquecraft.core.framework.command;

import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;

/**
 * Declarative description of a command. Everything the framework needs to register it and to
 * document it in help output lives here.
 *
 * @param name        the literal used to run the command
 * @param aliases     extra literals that run the same command
 * @param description short Spanish description shown in help output
 * @param permission  vanilla permission level required when no permission manager grants or denies the node
 */
public record CommandMetadata(String name, List<String> aliases, String description, PermissionLevel permission) {

    public CommandMetadata {
        aliases = List.copyOf(aliases);
    }

    public static CommandMetadata of(String name, String description, PermissionLevel permission) {
        return new CommandMetadata(name, List.of(), description, permission);
    }

    public CommandMetadata withAliases(String... aliases) {
        return new CommandMetadata(name, List.of(aliases), description, permission);
    }
}
