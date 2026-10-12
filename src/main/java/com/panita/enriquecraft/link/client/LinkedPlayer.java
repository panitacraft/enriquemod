package com.panita.enriquecraft.link.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.auth.data.AuthMode;

/**
 * A player the Panita API says may play: an active account with a Minecraft name and a known way to log in.
 *
 * @param userId    the stable id of the Panita account; it tells a renamed player from a removed one
 * @param ign       the Minecraft name, exactly as the member gave it
 * @param mode      how the name logs in: with a Microsoft account or with a PIN
 * @param discordId the Discord account of the member, kept for the Discord module
 */
public record LinkedPlayer(int userId, String ign, AuthMode mode, String discordId) {

    public static final Codec<LinkedPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("userId").forGetter(LinkedPlayer::userId),
            Codec.STRING.fieldOf("ign").forGetter(LinkedPlayer::ign),
            AuthMode.CODEC.fieldOf("mode").forGetter(LinkedPlayer::mode),
            Codec.STRING.fieldOf("discordId").forGetter(LinkedPlayer::discordId)
    ).apply(instance, LinkedPlayer::new));
}
