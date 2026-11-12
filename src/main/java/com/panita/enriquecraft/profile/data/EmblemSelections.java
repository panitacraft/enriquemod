package com.panita.enriquecraft.profile.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What each player chose to show of their emblems: the ones that go next to their name, in the order they chose, and which of their
 * prank levels goes before it. It is the mod's own data: it is not the website's layout and is never sent to the API. A player is told
 * by their Panita account, not by their UUID, so changing between a premium and a PIN login (which changes the UUID) does not lose the
 * choice.
 *
 * @param entries one entry for every player who chose emblems, in the order they first did
 * @param pranks  one entry for every player who chose a prank level
 */
public record EmblemSelections(List<Entry> entries, List<Prank> pranks) {

    public static final EmblemSelections EMPTY = new EmblemSelections(List.of(), List.of());

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

    /**
     * @param userId the Panita account
     * @param emblem the id of the prank emblem the player chose to show
     */
    public record Prank(int userId, String emblem) {

        public static final Codec<Prank> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("userId").forGetter(Prank::userId),
                Codec.STRING.fieldOf("emblem").forGetter(Prank::emblem)
        ).apply(instance, Prank::new));
    }

    public static final Codec<EmblemSelections> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(EmblemSelections::entries),
            // A file written before the prank could be chosen has no such list
            Prank.CODEC.listOf().optionalFieldOf("pranks", List.of()).forGetter(EmblemSelections::pranks)
    ).apply(instance, EmblemSelections::new));

    public EmblemSelections {
        entries = List.copyOf(entries);
        pranks = List.copyOf(pranks);
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
        return new EmblemSelections(updated, pranks);
    }

    /** The same choices without this player's emblems, so they are back to the default. Their prank level is kept. */
    public EmblemSelections without(int userId) {
        return new EmblemSelections(entries.stream().filter(entry -> entry.userId() != userId).toList(), pranks);
    }

    /** The prank level the player chose; empty when they never did, which shows the default level. */
    public Optional<String> prankOf(int userId) {
        return pranks.stream().filter(prank -> prank.userId() == userId).findFirst().map(Prank::emblem);
    }

    /** The same choices with this player's prank level replaced. */
    public EmblemSelections withPrank(int userId, String emblem) {
        List<Prank> updated = new ArrayList<>(pranks);
        updated.removeIf(prank -> prank.userId() == userId);
        updated.add(new Prank(userId, emblem));
        return new EmblemSelections(entries, updated);
    }

    /** The same choices without this player's prank level, so they are back to the default one. */
    public EmblemSelections withoutPrank(int userId) {
        return new EmblemSelections(entries, pranks.stream().filter(prank -> prank.userId() != userId).toList());
    }
}
