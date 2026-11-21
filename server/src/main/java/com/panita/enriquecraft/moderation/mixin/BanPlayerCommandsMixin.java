package com.panita.enriquecraft.moderation.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.panita.enriquecraft.moderation.ModerationHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.BanPlayerCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the game from registering its {@code /ban}, which has no time and no way to add one, so the module's own takes its
 * place with the same argument for the player and the same permission level. The ban list stays the game's.
 */
@Mixin(BanPlayerCommands.class)
public abstract class BanPlayerCommandsMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void enriquecraft$leaveItToTheModule(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        if (ModerationHooks.get() != null) {
            callback.cancel();
        }
    }
}
