package com.panita.enriquecraft.qol.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;
import com.panita.enriquecraft.qol.service.ShareSounds;

/**
 * Settings of the quality-of-life module, stored in the {@code qol} section of the config file. The texts of its messages are in
 * the messages file.
 */
public final class QolConfig implements ModConfig {

    private static final String SOUND_RULE = "must be the id of a sound of the game, such as minecraft:entity.player.levelup";
    private static final String DEFAULT_SOUND = "minecraft:entity.player.levelup";

    public final ConfigValue<Boolean> shareCoordinatesEnabled;
    public final ConfigValue<Boolean> shareCoordinatesPlaySound;
    public final ConfigValue<String> shareCoordinatesSoundPublic;
    public final ConfigValue<String> shareCoordinatesSoundPrivate;

    public QolConfig(ConfigSectionBuilder builder) {
        shareCoordinatesEnabled = builder.bool("shareCoordinates.enabled", true,
                "Lets players use /sharecoordinates (also /c, /coords and /coordinateshare) to tell everyone, or one player, where they are.");
        shareCoordinatesPlaySound = builder.bool("shareCoordinates.playSound", true,
                "Plays a sound to the players who receive the coordinates.");
        shareCoordinatesSoundPublic = builder.string("shareCoordinates.soundPublic", DEFAULT_SOUND,
                "The sound every player hears when someone shares their coordinates with everyone.",
                ShareSounds::isValid, SOUND_RULE);
        shareCoordinatesSoundPrivate = builder.string("shareCoordinates.soundPrivate", DEFAULT_SOUND,
                "The sound the sender and the receiver hear when coordinates are shared with one player (or with oneself).",
                ShareSounds::isValid, SOUND_RULE);
    }
}
