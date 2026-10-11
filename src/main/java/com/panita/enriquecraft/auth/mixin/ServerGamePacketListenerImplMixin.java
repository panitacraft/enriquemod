package com.panita.enriquecraft.auth.mixin;

import com.panita.enriquecraft.auth.AuthHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts a player who leaves while waiting for the PIN screen back as they really were, before the game saves
 * their data.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "removePlayerFromWorld", at = @At("HEAD"))
    private void enriquecraft$restoreBeforeSaving(CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks != null) {
            hooks.leaving(getPlayer());
        }
    }
}
