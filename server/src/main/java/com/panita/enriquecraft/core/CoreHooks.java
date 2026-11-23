package com.panita.enriquecraft.core;

import com.mojang.brigadier.CommandDispatcher;
import com.panita.enriquecraft.core.dialog.DialogService;
import com.panita.enriquecraft.core.framework.command.CommandRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The way the core module's mixins reach its services. A mixin is instantiated by Mixin, not by the service
 * registry, so this is the one static door, installed once when the module loads. Without it every mixin leaves
 * vanilla behavior untouched.
 */
public final class CoreHooks {

    private static volatile @Nullable CoreHooks instance;

    private final DialogService dialogs;
    private final CommandRegistry commands;

    private CoreHooks(DialogService dialogs, CommandRegistry commands) {
        this.dialogs = dialogs;
        this.commands = commands;
    }

    static void install(DialogService dialogs, CommandRegistry commands) {
        instance = new CoreHooks(dialogs, commands);
    }

    public static @Nullable CoreHooks get() {
        return instance;
    }

    /**
     * A client reported a custom click action, which the game gives no player. Returns whether it was a dialog's
     * answer, in which case the game must not handle it further.
     */
    public boolean customClickAction(ServerPlayer player, Identifier id, Optional<Tag> payload) {
        return dialogs.handleClick(player, id, payload);
    }

    /** The game finished building its command tree, with the commands of every mod in it. */
    public void commandsBuilt(CommandDispatcher<CommandSourceStack> dispatcher) {
        commands.claimOverridden(dispatcher);
    }
}
