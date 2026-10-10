package com.panita.enriquecraft.auth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.panita.enriquecraft.auth.AuthHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps the departure of a player who never entered their PIN quiet, as their arrival was, and puts them
 * back where they really were before their data is saved.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public abstract ServerPlayer getPlayer();

    @WrapOperation(method = "removePlayerFromWorld",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void enriquecraft$keepLeaveQuiet(PlayerList instance, Component message, boolean overlay,
                                             Operation<Void> original) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks == null || !hooks.leaving(getPlayer())) {
            original.call(instance, message, overlay);
        }
    }
}
