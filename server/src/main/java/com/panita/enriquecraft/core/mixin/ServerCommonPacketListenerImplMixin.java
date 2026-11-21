package com.panita.enriquecraft.core.mixin;

import com.panita.enriquecraft.core.CoreHooks;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game hands a custom click action (what a dialog button sends) to the server without saying who sent it, and
 * the server only logs it. This gives the core module the player, after the game has moved the packet to the server
 * thread and just before it would be logged.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    @Inject(method = "handleCustomClickAction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"),
            cancellable = true)
    private void enriquecraft$readDialogAnswers(ServerboundCustomClickActionPacket packet, CallbackInfo callback) {
        CoreHooks hooks = CoreHooks.get();
        if (hooks != null && (Object) this instanceof ServerGamePacketListenerImpl game
                && hooks.customClickAction(game.getPlayer(), packet.id(), packet.payload())) {
            callback.cancel();
        }
    }
}
