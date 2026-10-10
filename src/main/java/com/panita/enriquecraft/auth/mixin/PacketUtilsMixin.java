package com.panita.enriquecraft.auth.mixin;

import com.panita.enriquecraft.auth.AuthHooks;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The one door every packet of a player in the game goes through before it is handled. A player who has
 * not entered their PIN yet may only use the PIN screen, so the rest is dropped here, in the same quiet
 * way the game drops a packet it will handle on another thread: nothing they send can move them, build,
 * chat, run commands or touch anything.
 */
@Mixin(PacketUtils.class)
public abstract class PacketUtilsMixin {

    @Inject(method = "ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            at = @At("HEAD"))
    private static <T extends PacketListener> void enriquecraft$dropFromHeldPlayers(Packet<T> packet, T listener,
                                                                                    PacketProcessor processor,
                                                                                    CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks != null && listener instanceof ServerGamePacketListenerImpl game && hooks.blocks(game.getPlayer(), packet)) {
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }
}
