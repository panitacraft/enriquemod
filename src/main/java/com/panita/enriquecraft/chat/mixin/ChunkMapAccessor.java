package com.panita.enriquecraft.chat.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives the chat module the game's record of which entity is tracked for whom, so it can make the clients of nearby players
 * see a player again under a new name.
 */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {

    /** The tracking record of each entity, by the id of the entity. */
    @Accessor("entityMap")
    Int2ObjectMap<?> enriquecraft$trackedEntities();
}
