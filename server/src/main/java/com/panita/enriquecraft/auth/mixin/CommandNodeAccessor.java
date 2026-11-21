package com.panita.enriquecraft.auth.mixin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the auth module change what an existing command node runs, which Brigadier offers no way to do.
 * Used to take over the executors of {@code /whitelist add} and {@code remove} while keeping their
 * arguments, suggestions and permissions exactly as the game defines them.
 */
@Mixin(CommandNode.class)
public interface CommandNodeAccessor {

    @Accessor("command")
    <S> void enriquecraft$setCommand(Command<S> command);
}
