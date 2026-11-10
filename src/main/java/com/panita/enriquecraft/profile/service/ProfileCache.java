package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ModProfile;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * What the Panita API last said of each player, and when it said it. It lives in memory only and outlives a player's visit, so
 * coming back soon is answered without asking the API again. A player the API said nothing of has no profile, but the moment it
 * said so is kept too, so that answer is not asked for again either.
 */
public final class ProfileCache {

    private final Supplier<Instant> clock;
    private final Map<UUID, ModProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> readAt = new ConcurrentHashMap<>();

    public ProfileCache(Supplier<Instant> clock) {
        this.clock = clock;
    }

    public Optional<ModProfile> get(UUID player) {
        return Optional.ofNullable(profiles.get(player));
    }

    /** Stores what the API said of the player, now. */
    public void put(UUID player, ModProfile profile) {
        profiles.put(player, profile);
        readAt.put(player, clock.get());
    }

    /** Drops the player's profile because the API has none for them, and notes that it said so, now. */
    public void putAbsent(UUID player) {
        profiles.remove(player);
        readAt.put(player, clock.get());
    }

    /** Drops the player's profile without saying the API was asked, for a player who is no longer linked. */
    public void remove(UUID player) {
        profiles.remove(player);
        readAt.remove(player);
    }

    /** Whether the API answered about the player less than {@code maxAge} ago. */
    public boolean isFresh(UUID player, Duration maxAge) {
        Instant at = readAt.get(player);
        return at != null && Duration.between(at, clock.get()).compareTo(maxAge) < 0;
    }
}
