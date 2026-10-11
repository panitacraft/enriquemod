package com.panita.enriquecraft.auth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.panita.enriquecraft.auth.AuthHooks;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts a player who leaves while waiting for the PIN screen back as they really were, before the game saves
 * their data, and lets a player who waits in spectator mode use the PIN screen.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public abstract ServerPlayer getPlayer();

    @Shadow
    private SignedMessageChain.Decoder signedMessageDecoder;

    /**
     * The game turns a message down from a player without a profile key when the server asks for secure chat, which it
     * does whenever {@code enforce-secure-profile} and {@code online-mode} are on. A player who logs in with a PIN has no
     * Microsoft account behind the name, so they can never have a key: for them the game must accept unsigned messages
     * and commands, as it does on a server that is not in online mode.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enriquecraft$acceptUnsignedChatFromPinPlayers(MinecraftServer server, Connection connection, ServerPlayer player,
                                                               CommonListenerCookie cookie, CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks != null && hooks.hasNoProfileKey(player)) {
            signedMessageDecoder = SignedMessageChain.Decoder.unsigned(player.getUUID(), () -> false);
        }
    }

    /**
     * The game ignores every click of a spectator in a container. A player who waits in spectator mode must still
     * press the keys of the PIN screen, so for them the click is handled as for anyone else; the packet filter has
     * already limited what they can click to that screen.
     */
    @WrapOperation(method = "handleContainerClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean enriquecraft$letHeldSpectatorsPressKeys(ServerPlayer player, Operation<Boolean> original) {
        AuthHooks hooks = AuthHooks.get();
        return (hooks == null || !hooks.isHeld(player)) && original.call(player);
    }

    @Inject(method = "removePlayerFromWorld", at = @At("HEAD"))
    private void enriquecraft$restoreBeforeSaving(CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks != null) {
            hooks.leaving(getPlayer());
        }
    }
}
