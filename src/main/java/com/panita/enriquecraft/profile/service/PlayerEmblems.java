package com.panita.enriquecraft.profile.service;

import java.util.List;

/**
 * The emblems of one player as this server shows them.
 *
 * @param prank      the prank level, the one system emblem: always shown, first, and not up to the player
 * @param chosen     the ones the player chose to show next to their name, in order
 * @param selectable every emblem the player may choose from, whether or not they chose it, in the API's order
 */
public record PlayerEmblems(EmblemView prank, List<EmblemView> chosen, List<EmblemView> selectable) {

    public PlayerEmblems {
        chosen = List.copyOf(chosen);
        selectable = List.copyOf(selectable);
    }
}
