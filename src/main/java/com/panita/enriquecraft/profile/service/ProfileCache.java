package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.data.CachedProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What the Panita API last said of each player, and when it said it. It is saved in the world folder and outlives a player's visit
 * and a restart, so coming back soon is answered without asking the API again. A player the API said nothing of has no profile, but
 * the moment it said so is kept too, so that answer is not asked for again either.
 * <p>
 * The answers arrive on network threads, so every access is serialized; the file is rewritten only when something changed.
 */
public final class ProfileCache {

    private final Supplier<Instant> clock;
    private final Supplier<CachedProfiles> read;
    private final Consumer<CachedProfiles> write;

    public ProfileCache(Supplier<Instant> clock, Supplier<CachedProfiles> read, Consumer<CachedProfiles> write) {
        this.clock = clock;
        this.read = read;
        this.write = write;
    }

    public synchronized Optional<ModProfile> get(UUID player) {
        CachedProfiles.Entry entry = read.get().entries().get(player);
        return entry == null ? Optional.empty() : entry.profile();
    }

    /** Stores what the API said of the player, now. */
    public synchronized void put(UUID player, ModProfile profile) {
        set(player, new CachedProfiles.Entry(Optional.of(profile), clock.get()));
    }

    /** Notes that the API has no profile for the player, now. */
    public synchronized void putAbsent(UUID player) {
        set(player, new CachedProfiles.Entry(Optional.empty(), clock.get()));
    }

    /** Forgets the player entirely, for a player who is no longer linked. */
    public synchronized void remove(UUID player) {
        Map<UUID, CachedProfiles.Entry> entries = new HashMap<>(read.get().entries());
        if (entries.remove(player) != null) {
            write.accept(new CachedProfiles(entries));
        }
    }

    /** Whether the API answered about the player less than {@code maxAge} ago. */
    public synchronized boolean isFresh(UUID player, Duration maxAge) {
        CachedProfiles.Entry entry = read.get().entries().get(player);
        return entry != null && Duration.between(entry.readAt(), clock.get()).compareTo(maxAge) < 0;
    }

    private void set(UUID player, CachedProfiles.Entry entry) {
        Map<UUID, CachedProfiles.Entry> entries = new HashMap<>(read.get().entries());
        entries.put(player, entry);
        write.accept(new CachedProfiles(entries));
    }
}
