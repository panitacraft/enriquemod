package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.HeldPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.storage.LevelData;

import java.util.Optional;
import java.util.Set;

/**
 * Where players wait while they have not entered their PIN: the spawn of the server. Moving them there
 * keeps the world around their real position, and the coordinates, from someone who only knows a name.
 */
public final class Limbo {

    private Limbo() {
    }

    /** Moves the player to the waiting spot. */
    public static void enter(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        LevelData.RespawnData spawn = server.getRespawnData();
        ServerLevel level = Optional.ofNullable(server.getLevel(spawn.dimension())).orElse(server.overworld());
        BlockPos pos = spawn.pos();
        teleport(player, level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
    }

    /** Puts the player back where they really were; stays put when that dimension no longer exists. */
    public static void leave(ServerPlayer player, HeldPosition position) {
        ServerLevel level = player.level().getServer().getLevel(position.dimension());
        if (level != null) {
            teleport(player, level, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
        }
    }

    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw,
                                 float pitch) {
        player.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, true);
        player.resetFallDistance();
    }
}
