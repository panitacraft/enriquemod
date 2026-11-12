package com.panita.enriquecraft.profile.client;

import java.util.List;
import java.util.Optional;

/**
 * What the Panita API says of a player for this server: who they are and the emblems they hold that this server may show.
 *
 * @param userId      the Panita account
 * @param ign         the Minecraft name the member gave, if they gave one
 * @param discordName their Discord name
 * @param emblems     the emblems of the asked edition and the system emblems, in the API's order
 */
public record ModProfile(int userId, Optional<String> ign, String discordName, List<ApiEmblem> emblems) {

    public ModProfile {
        emblems = List.copyOf(emblems);
    }
}
