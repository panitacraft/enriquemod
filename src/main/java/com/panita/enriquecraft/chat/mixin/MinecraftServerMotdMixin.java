package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the chat module's text in the server list. The game builds the text of its status from a plain string, which cannot carry
 * colors beyond the legacy codes, so the finished status is rebuilt with a real component.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMotdMixin {

    @Inject(method = "buildServerStatus", at = @At("RETURN"), cancellable = true)
    private void enriquecraft$showTheModulesMotd(CallbackInfoReturnable<ServerStatus> callback) {
        ChatHooks hooks = ChatHooks.get();
        Component motd = hooks == null ? null : hooks.motd((MinecraftServer) (Object) this);
        if (motd != null) {
            ServerStatus status = callback.getReturnValue();
            callback.setReturnValue(new ServerStatus(motd, status.players(), status.version(), status.favicon(), status.enforcesSecureChat()));
        }
    }
}
