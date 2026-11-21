package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.data.ProfileDetails;
import com.panita.enriquecraft.profile.data.ProfileDetailsBook;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * What the rest of the mod asks about the details players share about themselves, and the only place that changes them. A player is
 * found by their Panita account, so one with no account has the default details and cannot change them.
 */
public final class ProfileDetailsService {

    /** What asking to save a text did. */
    public enum Outcome {
        SAVED,
        /** The text is longer than {@link ProfileDetails#BIO_MAX}. */
        TOO_LONG,
        /** The pronouns are longer than {@link ProfileDetails#PRONOUNS_MAX}. */
        PRONOUNS_TOO_LONG,
        /** The player has no Panita account to keep the details under. */
        NOT_LINKED
    }

    private final LinkedPlayers linked;
    private final Supplier<ProfileDetailsBook> read;
    private final Consumer<ProfileDetailsBook> write;

    public ProfileDetailsService(LinkedPlayers linked, Supplier<ProfileDetailsBook> read, Consumer<ProfileDetailsBook> write) {
        this.linked = linked;
        this.read = read;
        this.write = write;
    }

    /** The details of the player; the default ones when they have no account or never changed any. */
    public ProfileDetails detailsOf(ServerPlayer player) {
        return detailsOf(player.getGameProfile().name());
    }

    /** The details of the player with this Minecraft name, who may not be online. */
    public ProfileDetails detailsOf(String name) {
        return account(name).map(read.get()::of).orElse(ProfileDetails.DEFAULT);
    }

    /** Saves what the player wrote about themselves, cleaned by {@link BioText}. */
    public Outcome setBio(ServerPlayer player, String raw) {
        return setBio(player.getGameProfile().name(), raw);
    }

    Outcome setBio(String name, String raw) {
        Optional<String> bio = BioText.clean(raw);
        return bio.isEmpty() ? Outcome.TOO_LONG : change(name, details -> details.withBio(bio.get()));
    }

    /** Saves the pronouns the player chose to show, cleaned by {@link BioText}. */
    public Outcome setPronouns(ServerPlayer player, String raw) {
        return setPronouns(player.getGameProfile().name(), raw);
    }

    Outcome setPronouns(String name, String raw) {
        Optional<String> pronouns = BioText.pronouns(raw);
        return pronouns.isEmpty() ? Outcome.PRONOUNS_TOO_LONG : change(name, details -> details.withPronouns(pronouns.get()));
    }

    /** Marks the block the player stands on, in the dimension they are in, as their base. */
    public Outcome markBase(ServerPlayer player) {
        return setBase(player.getGameProfile().name(), Optional.of(ProfileDetails.Base.at(player.level().dimension(), player.blockPosition())));
    }

    public Outcome clearBase(ServerPlayer player) {
        return setBase(player.getGameProfile().name(), Optional.empty());
    }

    Outcome setBase(String name, Optional<ProfileDetails.Base> base) {
        return change(name, details -> details.withBase(base));
    }

    public Outcome setShareLocation(ServerPlayer player, boolean shared) {
        return setShareLocation(player.getGameProfile().name(), shared);
    }

    Outcome setShareLocation(String name, boolean shared) {
        return change(name, details -> details.withShareLocation(shared));
    }

    private Outcome change(String name, UnaryOperator<ProfileDetails> change) {
        Optional<Integer> account = account(name);
        if (account.isEmpty()) {
            return Outcome.NOT_LINKED;
        }
        ProfileDetailsBook book = read.get();
        write.accept(book.with(account.get(), change.apply(book.of(account.get()))));
        return Outcome.SAVED;
    }

    private Optional<Integer> account(String name) {
        return linked.byName(name).map(player -> player.userId());
    }
}
