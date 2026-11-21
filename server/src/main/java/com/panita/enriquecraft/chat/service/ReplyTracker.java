package com.panita.enriquecraft.chat.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Who each player last exchanged a private message with, so {@code /r} knows who to answer. Both of them: whoever writes
 * and whoever receives can answer. Nothing is stored; used from the server thread only.
 */
public final class ReplyTracker {

    private final Map<UUID, UUID> lastContact = new HashMap<>();

    public void exchanged(UUID from, UUID to) {
        lastContact.put(from, to);
        lastContact.put(to, from);
    }

    public Optional<UUID> lastContactOf(UUID player) {
        return Optional.ofNullable(lastContact.get(player));
    }

    /** A player leaves: nobody can answer them any more, and they have nobody to answer. */
    public void forget(UUID player) {
        lastContact.remove(player);
        lastContact.values().removeIf(player::equals);
    }
}
