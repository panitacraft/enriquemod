package com.panita.enriquecraft.profile.service;

import java.util.List;

/**
 * The emblems of one player as this server shows them.
 *
 * @param prank      the prank level shown before the name, the one system emblem: the level the player chose, or the first one
 * @param pranks     every prank level, lowest first, which the player may choose from whether or not it is the one shown
 * @param chosen     the ones the player chose to show next to their name, in order
 * @param empty      one stand-in for each of the places after the name that the player has not filled
 * @param selectable every emblem the player holds and may choose to show next to their name, in the API's order
 */
public record PlayerEmblems(EmblemView prank, List<EmblemView> pranks, List<EmblemView> chosen, List<EmblemView> empty,
                            List<EmblemView> selectable) {

    public PlayerEmblems {
        pranks = List.copyOf(pranks);
        chosen = List.copyOf(chosen);
        empty = List.copyOf(empty);
        selectable = List.copyOf(selectable);
    }
}
