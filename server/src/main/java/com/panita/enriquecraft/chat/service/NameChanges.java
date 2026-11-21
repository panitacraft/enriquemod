package com.panita.enriquecraft.chat.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Remembers what was last shown for each player, so only a change is sent again. Only the server thread uses it. */
public final class NameChanges {

    private final Map<UUID, String> shown = new HashMap<>();

    /** @return whether what is shown for the player differs from what was shown before, which is always so the first time */
    public boolean changed(UUID player, String signature) {
        return !signature.equals(shown.put(player, signature));
    }

    public void forget(UUID player) {
        shown.remove(player);
    }
}
