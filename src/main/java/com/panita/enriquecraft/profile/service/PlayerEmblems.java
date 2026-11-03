package com.panita.enriquecraft.profile.service;

import java.util.List;

/**
 * The emblems of one player as this server shows them.
 *
 * @param system     the ones shown with the system emblems, in order: the promoted emblems, then the nationality
 * @param chosen     the ones the player chose to show next to their name, in order
 * @param selectable every emblem the player may choose from, whether or not they chose it, in the API's order
 */
public record PlayerEmblems(List<EmblemView> system, List<EmblemView> chosen, List<EmblemView> selectable) {

    public static final PlayerEmblems NONE = new PlayerEmblems(List.of(), List.of(), List.of());

    public PlayerEmblems {
        system = List.copyOf(system);
        chosen = List.copyOf(chosen);
        selectable = List.copyOf(selectable);
    }
}
