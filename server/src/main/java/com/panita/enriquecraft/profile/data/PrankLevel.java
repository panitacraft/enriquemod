package com.panita.enriquecraft.profile.data;

import java.util.List;
import java.util.Locale;

/**
 * One prank level a player can choose, as the config declares it. The mod owns the levels: they do not come from the Panita API, and a
 * player may pick any of them unless the level names the players who may.
 *
 * @param id          the id of the level, which is also the id of the API's emblem for it, so that emblem is never offered as an
 *                    ordinary one
 * @param title       its title
 * @param description what it means, or empty for none
 * @param look        the character and the color it has for players without the client mod
 * @param image       the https address of its image on a trusted host, or empty for none
 * @param players     the Minecraft names of the only players who may choose it, ignoring capitalization; empty when anyone may
 */
public record PrankLevel(String id, String title, String description, EmblemLook look, String image, List<String> players) {

    public PrankLevel {
        players = List.copyOf(players);
    }

    /** Whether the player with this Minecraft name may choose the level. */
    public boolean mayBeChosenBy(String playerName) {
        return players.isEmpty() || players.stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).equals(playerName.toLowerCase(Locale.ROOT)));
    }
}
