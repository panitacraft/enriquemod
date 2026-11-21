package com.panita.enriquecraft.moderation.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Every mute that has been given and has not been lifted. A mute that ran out stays here until the next change, and is
 * ignored meanwhile.
 *
 * @param entries at most one for each player
 */
public record Mutes(List<MuteRecord> entries) {

    public static final Mutes EMPTY = new Mutes(List.of());

    public static final Codec<Mutes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MuteRecord.CODEC.listOf().fieldOf("entries").forGetter(Mutes::entries)
    ).apply(instance, Mutes::new));

    public Mutes {
        entries = List.copyOf(entries);
    }

    public Optional<MuteRecord> find(UUID id) {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    /** The same mutes with this one added, replacing the one of the same player. */
    public Mutes with(MuteRecord record) {
        List<MuteRecord> updated = new ArrayList<>(entries);
        updated.removeIf(entry -> entry.id().equals(record.id()));
        updated.add(record);
        return new Mutes(updated);
    }

    public Mutes without(Predicate<MuteRecord> removed) {
        return new Mutes(entries.stream().filter(removed.negate()).toList());
    }
}
