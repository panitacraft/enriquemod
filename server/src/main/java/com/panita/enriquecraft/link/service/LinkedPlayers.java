package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Which Panita account a player of the server is, for the modules that read more of it from the API. It answers from the last
 * list the link module applied, so it never waits for the network.
 */
public final class LinkedPlayers {

    private final Supplier<LinkedSnapshot> snapshot;

    public LinkedPlayers(Supplier<LinkedSnapshot> snapshot) {
        this.snapshot = snapshot;
    }

    /** The linked player with that Minecraft name, whatever its capitalization; empty for a player who is not on the list. */
    public Optional<LinkedPlayer> byName(String name) {
        return snapshot.get().players().stream().filter(player -> player.ign().equalsIgnoreCase(name)).findFirst();
    }
}
