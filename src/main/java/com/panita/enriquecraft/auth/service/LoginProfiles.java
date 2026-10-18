package com.panita.enriquecraft.auth.service;

import com.mojang.authlib.GameProfile;
import org.jspecify.annotations.Nullable;

/**
 * Decides which profile a player keeps once the login is finished. A mod that gives the player another skin, such as
 * SkinsRestorer, hands its profile to the step that tells the client the login is done, but the game builds the player from
 * the profile it authenticated, so the skin was lost as soon as the player joined.
 */
public final class LoginProfiles {

    private LoginProfiles() {
    }

    /**
     * The profile the player is built from: the one the login finished with, as long as it is the same player under the same
     * name, so that nothing can change who joins, only how they look.
     */
    public static @Nullable GameProfile carry(@Nullable GameProfile authenticated, GameProfile finished) {
        boolean samePlayer = authenticated != null && authenticated.id().equals(finished.id()) && authenticated.name().equals(finished.name());
        return samePlayer ? finished : authenticated;
    }
}
