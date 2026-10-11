package com.panita.enriquecraft.auth.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * How a player really was when they joined and had to wait for the PIN screen in the waiting spot: where, and
 * in which game mode (the waiting spot may put them in spectator mode). It is kept on disk so a crash while
 * they wait cannot strand them there.
 */
public record HeldState(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch,
                        Optional<GameType> gameMode) {

    public static final Codec<HeldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(HeldState::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(HeldState::x),
            Codec.DOUBLE.fieldOf("y").forGetter(HeldState::y),
            Codec.DOUBLE.fieldOf("z").forGetter(HeldState::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(HeldState::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(HeldState::pitch),
            GameType.CODEC.optionalFieldOf("gameMode").forGetter(HeldState::gameMode)
    ).apply(instance, HeldState::new));

    public static HeldState of(ServerPlayer player) {
        return new HeldState(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), Optional.of(player.gameMode()));
    }
}
