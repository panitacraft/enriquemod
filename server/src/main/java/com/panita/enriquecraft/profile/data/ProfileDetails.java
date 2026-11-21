package com.panita.enriquecraft.profile.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * What a player tells others about themselves on this server: their pronouns, a short text, where their base is, and whether they show on the
 * locator bar of others. It is the mod's own data and is never sent to the API.
 *
 * @param base          where the player's base is, if they marked it
 * @param bio           what the player wrote about themselves; plain text, may have several lines, empty for none
 * @param pronouns      the pronouns the player chose to show; one line of free text, empty for none
 * @param shareLocation whether others see the player on their locator bar
 */
public record ProfileDetails(Optional<Base> base, String bio, String pronouns, boolean shareLocation) {

    /** What a player who never changed anything has: no base, no text, and visible, as in the game. */
    public static final ProfileDetails DEFAULT = new ProfileDetails(Optional.empty(), "", "", true);

    /** The longest text a player can write about themselves. */
    public static final int BIO_MAX = 256;

    /** The longest pronouns a player can show. */
    public static final int PRONOUNS_MAX = 9;

    /**
     * A place in the world, as the block the player stood on.
     *
     * @param dimension the dimension it is in
     */
    public record Base(ResourceKey<Level> dimension, int x, int y, int z) {

        public static final Codec<Base> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Base::dimension),
                Codec.INT.fieldOf("x").forGetter(Base::x),
                Codec.INT.fieldOf("y").forGetter(Base::y),
                Codec.INT.fieldOf("z").forGetter(Base::z)
        ).apply(instance, Base::new));

        public static Base at(ResourceKey<Level> dimension, BlockPos position) {
            return new Base(dimension, position.getX(), position.getY(), position.getZ());
        }
    }

    /** Only what differs from the default is written. */
    public static final Codec<ProfileDetails> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Base.CODEC.optionalFieldOf("base").forGetter(ProfileDetails::base),
            Codec.STRING.optionalFieldOf("bio", "").forGetter(ProfileDetails::bio),
            Codec.STRING.optionalFieldOf("pronouns", "").forGetter(ProfileDetails::pronouns),
            Codec.BOOL.optionalFieldOf("shareLocation", true).forGetter(ProfileDetails::shareLocation)
    ).apply(instance, ProfileDetails::new));

    public ProfileDetails withBase(Optional<Base> base) {
        return new ProfileDetails(base, bio, pronouns, shareLocation);
    }

    public ProfileDetails withBio(String bio) {
        return new ProfileDetails(base, bio, pronouns, shareLocation);
    }

    public ProfileDetails withPronouns(String pronouns) {
        return new ProfileDetails(base, bio, pronouns, shareLocation);
    }

    public ProfileDetails withShareLocation(boolean shareLocation) {
        return new ProfileDetails(base, bio, pronouns, shareLocation);
    }
}
