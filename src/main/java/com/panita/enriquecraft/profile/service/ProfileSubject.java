package com.panita.enriquecraft.profile.service;

import com.mojang.authlib.GameProfile;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.data.ProfileDetails;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Everything a profile screen shows about one player, gathered once so the screen only lays it out. The player may be online or not.
 *
 * @param name        the real Minecraft name
 * @param id          the player's UUID
 * @param online      whether the player is connected and past the PIN screen
 * @param skin        the profile that carries the skin the server shows, when it is known; empty when it never was
 * @param profile     what the Panita API last said of them; empty for a player with no account or no answer yet
 * @param emblems     the emblems they show and hold
 * @param details     what they wrote about themselves, their base and their locator choice
 * @param playSeconds how long they have played, when it is known
 * @param deaths      how many times they died, when it is known
 */
public record ProfileSubject(String name, UUID id, boolean online, Optional<GameProfile> skin, Optional<ModProfile> profile,
                             PlayerEmblems emblems, ProfileDetails details, OptionalLong playSeconds, OptionalLong deaths) {

    /** Whether the name belongs to a Microsoft account, which only staff are told. */
    public AuthMode mode() {
        return AuthMode.of(id, name);
    }
}
