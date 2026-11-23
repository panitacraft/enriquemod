package com.panita.enriquecraft.core.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.panita.enriquecraft.core.CoreHooks;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs at the very end of the construction of the game's command tree, after the mods that add commands in it. The mixin has a higher
 * priority number than the default so that it runs after the ones that add theirs at the same point (Carpet does).
 */
@Mixin(value = Commands.class, priority = 2000)
public abstract class CommandsMixin {

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void enriquecraft$claimOverriddenCommands(Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo callback) {
        CoreHooks hooks = CoreHooks.get();
        if (hooks != null) {
            hooks.commandsBuilt(dispatcher);
        }
    }
}
