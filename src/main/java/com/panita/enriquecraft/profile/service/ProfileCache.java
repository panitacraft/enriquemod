package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ModProfile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the Panita API last said of each player who is online. It lives in memory only, is filled when a player is ready, when they
 * open their menu and when somebody refreshes it, and is dropped when they leave. A player the API said nothing of has no entry.
 */
public final class ProfileCache {

    private final Map<UUID, ModProfile> profiles = new ConcurrentHashMap<>();

    public Optional<ModProfile> get(UUID player) {
        return Optional.ofNullable(profiles.get(player));
    }

    public void put(UUID player, ModProfile profile) {
        profiles.put(player, profile);
    }

    public void remove(UUID player) {
        profiles.remove(player);
    }
}
