package com.panita.enriquecraft.moderation.mixin;

import com.panita.enriquecraft.moderation.ModerationHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.KickCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Makes the screen of a kicked player the mod prefix with the reason below it. Only the screen changes: the arguments, the
 * permission and the checks of {@code /kick} stay the game's.
 */
@Mixin(KickCommand.class)
public abstract class KickCommandMixin {

    @ModifyArg(method = "kickPlayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"))
    private static Component enriquecraft$putTheReasonUnderThePrefix(Component reason) {
        ModerationHooks hooks = ModerationHooks.get();
        return hooks == null ? reason : hooks.kickScreen(reason);
    }
}
