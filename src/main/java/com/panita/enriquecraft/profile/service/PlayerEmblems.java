package com.panita.enriquecraft.profile.service;

import java.util.List;
import java.util.Optional;

/**
 * The emblems of one player as this server shows them.
 *
 * @param nationality the nationality emblem the Panita account has, which the player cannot change in game; empty for none
 * @param prank      the prank level shown before the name, the one system emblem: the level the player chose, or the first one
 * @param pranks     every prank level, lowest first, which the player may choose from whether or not it is the one shown
 * @param chosen     the ones the player chose to show next to their name, in order, without the places they left empty
 * @param slots      every place after the name, in order: the emblem the player put there or a stand-in for an empty place, so the
 *                   player can leave any place empty and fill another
 * @param selectable every emblem the player holds and may choose to show next to their name, in the API's order
 */
public record PlayerEmblems(Optional<EmblemView> nationality, EmblemView prank, List<EmblemView> pranks, List<EmblemView> chosen, List<EmblemView> slots,
                            List<EmblemView> selectable) {

    public PlayerEmblems {
        pranks = List.copyOf(pranks);
        chosen = List.copyOf(chosen);
        slots = List.copyOf(slots);
        selectable = List.copyOf(selectable);
    }

    /** The id in each place, in order; an empty string for a place with nothing in it. */
    public List<String> placedIds() {
        return slots.stream().map(slot -> slot.id().equals(EmblemResolver.EMPTY_ID) ? "" : slot.id()).toList();
    }
}
