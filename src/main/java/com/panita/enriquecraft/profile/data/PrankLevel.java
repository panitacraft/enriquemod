package com.panita.enriquecraft.profile.data;

/**
 * One prank level a player can choose, as the config declares it. The mod owns the levels: they do not come from the Panita API, and a
 * player may pick any of them.
 *
 * @param id          the id of the level, which is also the id of the API's emblem for it, so that emblem is never offered as an
 *                    ordinary one
 * @param title       its title
 * @param description what it means, or empty for none
 * @param look        the character and the color it has for players without the client mod
 * @param image       the https address of its image on a trusted host, or empty for none
 */
public record PrankLevel(String id, String title, String description, EmblemLook look, String image) {
}
