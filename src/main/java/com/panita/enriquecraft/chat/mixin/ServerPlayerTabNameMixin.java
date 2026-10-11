package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shows the nickname of a player in the tab list. Players without one keep the game's own entry.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabNameMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$showTheNickname(CallbackInfoReturnable<Component> callback) {
        ChatHooks hooks = ChatHooks.get();
        Component nickname = hooks == null ? null : hooks.nicknameOf((ServerPlayer) (Object) this);
        if (nickname != null) {
            callback.setReturnValue(nickname);
        }
    }
}
