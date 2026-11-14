package com.panita.enriquecraft.moderation.mixin;

import com.panita.enriquecraft.moderation.ModerationHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/**
 * Replaces the screen of a banned player who tries to log in with the module's: the mod prefix, how long the ban still lasts
 * and why. The decision to refuse stays the game's.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListBanScreenMixin {

    @Shadow
    public abstract UserBanList getBans();

    @Inject(method = "canPlayerLogin", at = @At("RETURN"), cancellable = true)
    private void enriquecraft$showTheBanUnderThePrefix(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> callback) {
        ModerationHooks hooks = ModerationHooks.get();
        if (hooks == null || callback.getReturnValue() == null || !getBans().isBanned(profile)) {
            return;
        }
        hooks.banScreen(getBans(), profile).ifPresent(callback::setReturnValue);
    }
}
