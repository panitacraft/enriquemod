package com.panita.enriquecraft.staff.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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
 * @param iconPlayer  when the icon is a player head, the player whose head it is
 */
public record SavedCoordinate(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
                              float pitch, UUID savedBy, String savedByName, Instant savedAt, Item icon,
                              String displayName, Optional<PlayerRef> iconPlayer) {

    /** The longest player name. */
    public static final int MAX_PLAYER_NAME = 16;

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

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
                            ? Optional.empty() : Optional.of(coordinate.displayName())),
            PlayerRef.CODEC.optionalFieldOf("iconPlayer").forGetter(SavedCoordinate::iconPlayer)
    ).apply(instance, (name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName, iconPlayer) ->
            new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon,
                    displayName.orElse(name), iconPlayer)));

    /** A coordinate shown under its id, as a new one is. */
    public SavedCoordinate(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
                           float pitch, UUID savedBy, String savedByName, Instant savedAt, Item icon) {
        this(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, name);
    }

    /** A coordinate whose icon is an ordinary item. */
    public SavedCoordinate(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
                           float pitch, UUID savedBy, String savedByName, Instant savedAt, Item icon,
                           String displayName) {
        this(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName, Optional.empty());
    }

    /** Whether text can be used as a display name: not blank, at most {@link #MAX_DISPLAY_NAME} characters. */
    public static boolean isValidDisplayName(String text) {
        return !text.isBlank() && text.length() <= MAX_DISPLAY_NAME && text.chars().noneMatch(Character::isISOControl);
    }

    /** Whether text can be a player name: 3 to 16 letters, digits or underscores. */
    public static boolean isValidPlayerName(String text) {
        return PLAYER_NAME.matcher(text).matches();
    }

    /** The same coordinate, shown as another item. */
    public SavedCoordinate withIcon(Item icon) {
        return new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName,
                Optional.empty());
    }

    /** The same coordinate, shown as the head of a player. */
    public SavedCoordinate withHeadOf(PlayerRef owner) {
        return new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, Items.PLAYER_HEAD,
                displayName, Optional.of(owner));
    }

    /** The icon as an item stack, which is the head of the chosen player when it is one. */
    public ItemStack iconStack() {
        return iconPlayer.map(owner -> PlayerHeads.item(owner.id())).orElseGet(() -> new ItemStack(icon));
    }

    /** The same coordinate under another display name; the id stays. */
    public SavedCoordinate withDisplayName(String displayName) {
        return new SavedCoordinate(name, dimension, x, y, z, yaw, pitch, savedBy, savedByName, savedAt, icon, displayName,
                iconPlayer);
    }

    /** Saves the exact spot where a player stands now. */
    public static SavedCoordinate at(ServerPlayer player, String name, Item icon) {
        return new SavedCoordinate(name, player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getUUID(), player.getName().getString(), Instant.now(), icon);
    }
}
