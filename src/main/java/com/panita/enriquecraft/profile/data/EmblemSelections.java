package com.panita.enriquecraft.profile.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Which emblems each player chose to show next to their name, in the order they chose. It is the mod's own data: it is not the
 * website's layout and is never sent to the API. A player is told by their Panita account, not by their UUID, so changing between
 * a premium and a PIN login (which changes the UUID) does not lose the choice.
 *
 * @param entries one entry for every player who chose, in the order they first did
 */
public record EmblemSelections(List<Entry> entries) {

    public static final EmblemSelections EMPTY = new EmblemSelections(List.of());

    /**
     * @param userId  the Panita account
     * @param emblems the ids of the emblems chosen, in order; empty when the player chose to show none
     */
    public record Entry(int userId, List<String> emblems) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("userId").forGetter(Entry::userId),
                Codec.STRING.listOf().fieldOf("emblems").forGetter(Entry::emblems)
        ).apply(instance, Entry::new));

        public Entry {
            emblems = List.copyOf(emblems);
        }
    }

    public static final Codec<EmblemSelections> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(EmblemSelections::entries)
    ).apply(instance, EmblemSelections::new));

    public EmblemSelections {
        entries = List.copyOf(entries);
    }

    /** What the player chose; empty when they never did, which is different from choosing to show none. */
    public Optional<List<String>> of(int userId) {
        return entries.stream().filter(entry -> entry.userId() == userId).findFirst().map(Entry::emblems);
    }

    /** The same choices with this player's replaced. */
    public EmblemSelections with(int userId, List<String> emblems) {
        List<Entry> updated = new ArrayList<>(entries);
        updated.removeIf(entry -> entry.userId() == userId);
        updated.add(new Entry(userId, emblems));
        return new EmblemSelections(updated);
    }

    /** The same choices without this player's, so they are back to the default. */
    public EmblemSelections without(int userId) {
        return new EmblemSelections(entries.stream().filter(entry -> entry.userId() != userId).toList());
    }
}
