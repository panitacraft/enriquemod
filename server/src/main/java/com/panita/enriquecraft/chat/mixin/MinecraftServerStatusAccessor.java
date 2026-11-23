package com.panita.enriquecraft.chat.mixin;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Gives the chat module the game's cached server list status, so it can build it again at once. The game only rebuilds it from
 * its tick, which does not run while the server is paused for having no players, and then only every few seconds.
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerStatusAccessor {

    @Accessor("status")
    void enriquecraft$setStatus(ServerStatus status);

    @Invoker("buildServerStatus")
    ServerStatus enriquecraft$buildServerStatus();
}
