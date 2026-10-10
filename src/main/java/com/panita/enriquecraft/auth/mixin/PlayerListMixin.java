package com.panita.enriquecraft.auth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.panita.enriquecraft.auth.AuthHooks;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the auth module look at a player the moment they are placed in the world, and keeps a player who
 * still has to enter their PIN from being announced: the others hear of them only once they are in.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void enriquecraft$checkPin(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
                                       CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks != null) {
            hooks.playerJoining(connection, player);
        }
    }

    @WrapOperation(method = "placeNewPlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void enriquecraft$keepJoinQuiet(PlayerList instance, Component message, boolean overlay,
                                            Operation<Void> original, @Local(argsOnly = true) ServerPlayer player) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks == null || !hooks.isHeld(player)) {
            original.call(instance, message, overlay);
        }
    }
}
