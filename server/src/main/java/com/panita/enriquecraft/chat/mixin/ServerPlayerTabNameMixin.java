package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shows the name the module builds for a player in the tab list or, when it does not build one, their nickname. Players with neither keep
 * the game's own entry.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabNameMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$showTheName(CallbackInfoReturnable<Component> callback) {
        ChatHooks hooks = ChatHooks.get();
        if (hooks == null) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        // The module's own line when it builds the tab list, else the nickname alone
        Component name = hooks.tabNameOf(player);
        Component shown = name != null ? name : hooks.nicknameOf(player);
        if (shown != null) {
            callback.setReturnValue(shown);
        }
    }
}
