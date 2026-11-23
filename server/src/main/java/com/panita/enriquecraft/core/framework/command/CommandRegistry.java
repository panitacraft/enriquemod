package com.panita.enriquecraft.core.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

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
        // A command that overrides another is added only at the end, by claimOverridden, so it is never merged with the one it displaces
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                catalog.roots().stream().filter(entry -> !entry.spec().overrides()).forEach(entry -> treeBuilder.registerOn(dispatcher, entry)));
    }

    /**
     * Called when the whole tree is built, after every mod has added its commands: puts the commands marked
     * {@link CommandSpec#overrides()} in place of any command of the same name.
     */
    public void claimOverridden(CommandDispatcher<CommandSourceStack> dispatcher) {
        catalog.roots().stream()
                .filter(entry -> entry.spec().overrides())
                .forEach(entry -> treeBuilder.replaceOn(dispatcher, entry));
    }
}
