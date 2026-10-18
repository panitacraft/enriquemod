package com.panita.enriquecraft.discord.mixin;

import com.panita.enriquecraft.discord.DiscordHooks;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the discord module see what a player does, to end the picture it shows on a map as soon as they do anything but move
 * or chat. The door is called twice for a packet, first on the network thread and then on the server thread, which is the
 * one where the packet is really handled and the only one that is told.
 */
@Mixin(PacketUtils.class)
public abstract class PacketUtilsMixin {

    @Inject(method = "ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            at = @At("HEAD"))
    private static <T extends PacketListener> void enriquecraft$seePlayerActivity(Packet<T> packet, T listener, PacketProcessor processor,
                                                                                  CallbackInfo callback) {
        DiscordHooks hooks = DiscordHooks.get();
        if (hooks != null && listener instanceof ServerGamePacketListenerImpl game && processor.isSameThread()) {
            hooks.packetReceived(game.getPlayer(), packet);
        }
    }
}
