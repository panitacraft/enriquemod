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
import java.util.Optional;
import java.util.UUID;

/**
 * A point of interest saved by a staff member, shared by all staff.
 *
 * @param name        the id it is looked up by in commands; it never changes
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
 * @param displayName the name shown in menus, which staff can edit; the id until they do
 */
public record SavedCoordinate(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
                              float pitch, UUID savedBy, String savedByName, Instant savedAt, Item icon,
                              String displayName) {

    /** The longest display name staff can give a coordinate. */
    public static final int MAX_DISPLAY_NAME = 32;

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
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(SavedCoordinate::icon),
            // Only written when it differs from the id, so files from before it existed still load.
            Codec.STRING.optionalFieldOf("displayName").forGetter(
                    coordinate -> coordinate.displayName().equals(coordinate.name())
                            ? Optional.empty() : Optional.of(coordinate.displayName()))
    ).apply(instance, (name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName) ->
            new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon,
                    displayName.orElse(name))));

    /** A coordinate shown under its id, as a new one is. */
    public SavedCoordinate(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
                           float pitch, UUID savedBy, String savedByName, Instant savedAt, Item icon) {
        this(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, name);
    }

    /** Whether text can be used as a display name: not blank, at most {@link #MAX_DISPLAY_NAME} characters. */
    public static boolean isValidDisplayName(String text) {
        return !text.isBlank() && text.length() <= MAX_DISPLAY_NAME && text.chars().noneMatch(Character::isISOControl);
    }

    /** The same coordinate, shown as another item. */
    public SavedCoordinate withIcon(Item icon) {
        return new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName);
    }

    /** The same coordinate under another display name; the id stays. */
    public SavedCoordinate withDisplayName(String displayName) {
        return new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName);
    }

    /** Saves the exact spot where a player stands now. */
    public static SavedCoordinate at(ServerPlayer player, String name, Item icon) {
        return new SavedCoordinate(name, player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getUUID(), player.getName().getString(), Instant.now(), icon);
    }
}
