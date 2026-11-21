package com.panita.enriquecraft.chat.service;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.panita.enriquecraft.chat.data.KnownSkins;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Remembers the skin each player last had, so a line written for them while they are not online (a Discord message) can still start
 * with their head. It is taken from the game profile, which carries the skin the server shows (SkinsRestorer puts the skin a player
 * chose there), when a player is let in and when they leave, so a skin changed while playing is the one kept. SkinsRestorer is not a
 * dependency, and nothing here asks it or Mojang anything.
 */
public final class SkinMemory {

    private static final String TEXTURES = "textures";

    private final Supplier<KnownSkins> read;
    private final Consumer<KnownSkins> write;

    public SkinMemory(Supplier<KnownSkins> read, Consumer<KnownSkins> write) {
        this.read = read;
        this.write = write;
    }

    /** Keeps the skin of this profile, if it has one. The file is rewritten only when something changed. */
    public synchronized void remember(GameProfile profile) {
        Optional<Property> textures = profile.properties().get(TEXTURES).stream().findFirst();
        if (textures.isEmpty()) {
            return;
        }
        KnownSkins.Entry entry = new KnownSkins.Entry(profile.name(), profile.id(), textures.get().value());
        if (!read.get().find(profile.name()).equals(Optional.of(entry))) {
            write.accept(read.get().with(entry));
        }
    }

    /** A profile with the skin last seen of the player, without a signature; empty when none was seen. */
    public synchronized Optional<GameProfile> profileOf(String name) {
        return read.get().find(name).map(entry -> new GameProfile(entry.id(), entry.name(),
                new PropertyMap(ImmutableMultimap.of(TEXTURES, new Property(TEXTURES, entry.textures())))));
    }
}
