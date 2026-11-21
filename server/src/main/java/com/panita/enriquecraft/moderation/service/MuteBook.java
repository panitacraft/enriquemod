package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.moderation.data.MuteRecord;
import com.panita.enriquecraft.moderation.data.Mutes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Who is muted and until when. A mute that has run out is no mute: it is ignored when asked about and dropped the next time
 * the book changes. Used from the server thread only.
 */
public final class MuteBook {

    private final Supplier<Mutes> read;
    private final Consumer<Mutes> write;
    private final Supplier<Instant> clock;

    public MuteBook(Supplier<Mutes> read, Consumer<Mutes> write, Supplier<Instant> clock) {
        this.read = read;
        this.write = write;
        this.clock = clock;
    }

    public Optional<MuteRecord> activeFor(UUID id) {
        return read.get().find(id).filter(record -> record.isActiveAt(clock.get()));
    }

    /** The names of the players who are muted now. */
    public List<String> mutedNames() {
        Instant now = clock.get();
        return read.get().entries().stream().filter(record -> record.isActiveAt(now)).map(MuteRecord::name).toList();
    }

    /**
     * @param duration how long it lasts; empty for permanent
     * @return the mute, or empty when the player is already muted and nothing changed
     */
    public Optional<MuteRecord> mute(UUID id, String name, Optional<Duration> duration, String source, Optional<String> reason) {
        if (activeFor(id).isPresent()) {
            return Optional.empty();
        }
        Instant now = clock.get();
        MuteRecord record = new MuteRecord(id, name, now, duration.map(now::plus), source, reason);
        write.accept(read.get().without(old -> !old.isActiveAt(now)).with(record));
        return Optional.of(record);
    }

    /**
     * Lifts the mute of a player.
     *
     * @return the mutes that were lifted; empty when nobody of that name was muted
     */
    public List<MuteRecord> unmute(String name) {
        Instant now = clock.get();
        List<MuteRecord> lifted = read.get().entries().stream()
                .filter(record -> record.isActiveAt(now) && record.name().equalsIgnoreCase(name))
                .toList();
        if (!lifted.isEmpty()) {
            write.accept(read.get().without(record -> lifted.contains(record) || !record.isActiveAt(now)));
        }
        return lifted;
    }
}
