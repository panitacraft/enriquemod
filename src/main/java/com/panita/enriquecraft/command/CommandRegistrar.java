package com.panita.enriquecraft.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Registers every command of a {@link CommandCatalog} when the server builds its command tree.
 */
public final class CommandRegistrar {

    private final CommandCatalog catalog;
    private final CommandTreeBuilder treeBuilder;

    public CommandRegistrar(CommandCatalog catalog, CommandTreeBuilder treeBuilder) {
        this.catalog = catalog;
        this.treeBuilder = treeBuilder;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                catalog.commands().forEach(command -> treeBuilder.registerOn(dispatcher, command)));
    }
}
