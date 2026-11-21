package com.panita.enriquecraft.chat.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The two things the game does when an entity comes into the view of a player and leaves it: tell their client the entity is
 * gone, and tell it the entity is there. Done one after the other they make the client forget what it knew about the entity.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityInvoker {

    @Invoker("removePlayer")
    void enriquecraft$stopShowingTo(ServerPlayer viewer);

    @Invoker("updatePlayer")
    void enriquecraft$showToIfInView(ServerPlayer viewer);
}
