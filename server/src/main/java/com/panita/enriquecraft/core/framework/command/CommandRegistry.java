package com.panita.enriquecraft.core.framework.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Registers every command of a {@link CommandCatalog} when the server builds its command tree.
 * The catalog is read at that moment, so commands added by any module before then are included.
 */
public final class CommandRegistry {

    private final CommandCatalog catalog;
    private final CommandTreeBuilder treeBuilder;

    public CommandRegistry(CommandCatalog catalog, CommandTreeBuilder treeBuilder) {
        this.catalog = catalog;
        this.treeBuilder = treeBuilder;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                catalog.roots().forEach(entry -> treeBuilder.registerOn(dispatcher, entry)));
    }
}
