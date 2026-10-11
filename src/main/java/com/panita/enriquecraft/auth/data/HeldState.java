package com.panita.enriquecraft.auth.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Where a player really was when they joined and had to wait for the PIN screen in the waiting spot.
 * It is kept on disk so a crash while they wait cannot strand them there.
 */
public record HeldPosition(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {

    public static final Codec<HeldPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(HeldPosition::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(HeldPosition::x),
            Codec.DOUBLE.fieldOf("y").forGetter(HeldPosition::y),
            Codec.DOUBLE.fieldOf("z").forGetter(HeldPosition::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(HeldPosition::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(HeldPosition::pitch)
    ).apply(instance, HeldPosition::new));

    public static HeldPosition of(ServerPlayer player) {
        return new HeldPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }
}
