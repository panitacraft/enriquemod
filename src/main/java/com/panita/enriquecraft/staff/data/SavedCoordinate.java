package com.panita.enriquecraft.staff.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.util.UUID;

/**
 * A point of interest saved by a staff member, shared by all staff.
 *
 * @param name        the name it is looked up by
 * @param dimension   the dimension it is in
 * @param x           the exact position
 * @param y           the exact position
 * @param z           the exact position
 * @param yaw         the direction the staff member was facing, restored on teleport
 * @param pitch       the vertical look angle, restored on teleport
 * @param savedBy     who saved it
 * @param savedByName that person's name when it was saved
 * @param savedAt     when it was saved
 * @param icon        the item that represents it in menus
 */
public record SavedCoordinate(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
                              float pitch, UUID savedBy, String savedByName, Instant savedAt, Item icon) {

    public static final Codec<SavedCoordinate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SavedCoordinate::name),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(SavedCoordinate::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(SavedCoordinate::x),
            Codec.DOUBLE.fieldOf("y").forGetter(SavedCoordinate::y),
            Codec.DOUBLE.fieldOf("z").forGetter(SavedCoordinate::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(SavedCoordinate::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(SavedCoordinate::pitch),
            UUIDUtil.STRING_CODEC.fieldOf("savedBy").forGetter(SavedCoordinate::savedBy),
            Codec.STRING.fieldOf("savedByName").forGetter(SavedCoordinate::savedByName),
            TimeCodecs.INSTANT.fieldOf("savedAt").forGetter(SavedCoordinate::savedAt),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(SavedCoordinate::icon)
    ).apply(instance, SavedCoordinate::new));

    /** Saves the exact spot where a player stands now. */
    public static SavedCoordinate at(ServerPlayer player, String name, Item icon) {
        return new SavedCoordinate(name, player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getUUID(), player.getName().getString(), Instant.now(), icon);
    }
}
