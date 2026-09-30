package com.panita.enriquecraft.staff.service;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Moves players to an exact spot in any dimension.
 */
public final class Teleporter {

    public enum Result { TELEPORTED, DIMENSION_UNAVAILABLE }

    private Teleporter() {
    }

    /**
     * Teleports a player to the exact position, facing the given way. Nothing happens when the
     * dimension does not exist on this server.
     */
    public static Result teleport(ServerPlayer player, ResourceKey<Level> dimension, double x, double y, double z,
                                  float yaw, float pitch) {
        ServerLevel level = player.level().getServer().getLevel(dimension);
        if (level == null) {
            return Result.DIMENSION_UNAVAILABLE;
        }
        player.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, true);
        return Result.TELEPORTED;
    }
}
