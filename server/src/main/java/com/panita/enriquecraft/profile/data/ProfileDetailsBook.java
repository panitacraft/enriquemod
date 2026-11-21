package com.panita.enriquecraft.profile.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The details of every player who changed any, told by their Panita account and not by their UUID, so changing between a premium and
 * a PIN login (which changes the UUID) does not lose them.
 *
 * @param entries one entry for every player whose details differ from the default
 */
public record ProfileDetailsBook(List<Entry> entries) {

    public static final ProfileDetailsBook EMPTY = new ProfileDetailsBook(List.of());

    /** @param userId the Panita account */
    public record Entry(int userId, ProfileDetails details) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("userId").forGetter(Entry::userId),
                ProfileDetails.CODEC.fieldOf("details").forGetter(Entry::details)
        ).apply(instance, Entry::new));
    }

    public static final Codec<ProfileDetailsBook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(ProfileDetailsBook::entries)
    ).apply(instance, ProfileDetailsBook::new));

    public ProfileDetailsBook {
        entries = List.copyOf(entries);
    }

    /** The details of the player; the default ones when they never changed any. */
    public ProfileDetails of(int userId) {
        return entries.stream().filter(entry -> entry.userId() == userId).findFirst().map(Entry::details).orElse(ProfileDetails.DEFAULT);
    }

    /** The same book with this player's details replaced. Details equal to the default are not kept, so the file stays small. */
    public ProfileDetailsBook with(int userId, ProfileDetails details) {
        List<Entry> updated = new ArrayList<>(entries);
        updated.removeIf(entry -> entry.userId() == userId);
        if (!details.equals(ProfileDetails.DEFAULT)) {
            updated.add(new Entry(userId, details));
        }
        return new ProfileDetailsBook(updated);
    }
}
