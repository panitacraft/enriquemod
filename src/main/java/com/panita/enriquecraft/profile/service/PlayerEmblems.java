package com.panita.enriquecraft.profile.service;

import java.util.List;

/**
 * The emblems of one player as this server shows them.
 *
 * @param prank      the prank level shown before the name, the one system emblem: the one the player chose among their levels, or the
 *                   default level
 * @param pranks     the prank levels the player may choose from, lowest first, whether or not they chose one; the default level is not
 *                   among them
 * @param chosen     the ones the player chose to show next to their name, in order
 * @param selectable every other emblem the player may choose from, whether or not they chose it, in the API's order
 */
public record PlayerEmblems(EmblemView prank, List<EmblemView> pranks, List<EmblemView> chosen, List<EmblemView> selectable) {

    public PlayerEmblems {
        pranks = List.copyOf(pranks);
        chosen = List.copyOf(chosen);
        selectable = List.copyOf(selectable);
    }
}
