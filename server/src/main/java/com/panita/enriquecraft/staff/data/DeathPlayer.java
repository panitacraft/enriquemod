package com.panita.enriquecraft.staff.data;

import java.time.Instant;
import java.util.UUID;

/**
 * A player who has died at least once, as the list of players with deaths shows them.
 *
 * @param id        the player
 * @param name      their name at their latest death
 * @param lastDeath when they last died
 * @param deaths    how many deaths are kept for them
 */
public record DeathPlayer(UUID id, String name, Instant lastDeath, int deaths) {
}
