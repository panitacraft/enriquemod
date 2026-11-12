package com.panita.enriquecraft.chat.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The skin each player last had when the server saw them, so their head can be drawn when they are not online. It is the
 * {@code textures} value of their game profile, which is the skin the server shows them with (SkinsRestorer writes the skin a player
 * chose there), kept by name because that is all a Discord message says of its author.
 *
 * @param entries one entry for every player seen, in the order they were first seen
 */
public record KnownSkins(List<Entry> entries) {

    public static final KnownSkins EMPTY = new KnownSkins(List.of());

    /**
     * @param name     their name as the game profile had it
     * @param id       their id, which the client uses to pick a default skin when there is none
     * @param textures the {@code textures} value of their profile, without its signature
     */
    public record Entry(String name, UUID id, String textures) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                Codec.STRING.fieldOf("textures").forGetter(Entry::textures)
        ).apply(instance, Entry::new));
    }

    public static final Codec<KnownSkins> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(KnownSkins::entries)
    ).apply(instance, KnownSkins::new));

    public KnownSkins {
        entries = List.copyOf(entries);
    }

    public Optional<Entry> find(String name) {
        return entries.stream().filter(entry -> entry.name().equalsIgnoreCase(name)).findFirst();
    }

    /** The same skins with this one added, or replacing the one of the same name. */
    public KnownSkins with(Entry entry) {
        List<Entry> updated = new ArrayList<>(entries);
        updated.removeIf(known -> known.name().equalsIgnoreCase(entry.name()));
        updated.add(entry);
        return new KnownSkins(updated);
    }
}
