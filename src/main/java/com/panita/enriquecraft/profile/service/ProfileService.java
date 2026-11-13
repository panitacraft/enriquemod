package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * What the rest of the mod asks about a player's profile: their emblems as this server shows them, who they are on Panitacraft,
 * and what they chose to show. It reads the cache and the choices; it never waits for the network.
 */
public final class ProfileService {

    /** What asking to change the prank level did. */
    public record PrankChange(Status status, Duration remaining) {

        public enum Status {
            CHANGED,
            /** The player is already at that level. */
            SAME,
            /** The id is not one of the levels. */
            UNKNOWN,
            /** The level is the starting one, which cannot be chosen, so a player who has left it cannot return. */
            LOCKED,
            /** The last change was too recent; {@code remaining} says how long is left. */
            COOLDOWN,
            /** The player has no Panita account to keep the choice under. */
            NOT_LINKED
        }

        static PrankChange of(Status status) {
            return new PrankChange(status, Duration.ZERO);
        }
    }

    private final ProfileCache cache;
    private final EmblemResolver resolver;
    private final LinkedPlayers linked;
    private final Supplier<EmblemSelections> read;
    private final Consumer<EmblemSelections> write;
    private final Supplier<Instant> clock;
    private final IntSupplier prankCooldownSeconds;
    private final List<Consumer<ServerPlayer>> updates = new CopyOnWriteArrayList<>();

    public ProfileService(ProfileCache cache, EmblemResolver resolver, LinkedPlayers linked, Supplier<EmblemSelections> read,
                          Consumer<EmblemSelections> write, Supplier<Instant> clock, IntSupplier prankCooldownSeconds) {
        this.cache = cache;
        this.resolver = resolver;
        this.linked = linked;
        this.read = read;
        this.write = write;
        this.clock = clock;
        this.prankCooldownSeconds = prankCooldownSeconds;
    }

    public Optional<ModProfile> profileOf(UUID player) {
        return cache.get(player);
    }

    /**
     * The emblems of the player as this server shows them. The prank level does not need the API, only the Panita account the player
     * is listed under; the emblems after the name need what the API said of them.
     */
    public PlayerEmblems emblemsOf(ServerPlayer player) {
        EmblemSelections selections = read.get();
        Optional<String> prank = accountOf(player).flatMap(selections::prankOf).map(EmblemSelections.Prank::level);
        return cache.get(player.getUUID())
                .map(profile -> resolver.resolve(profile, selections.of(profile.userId()), prank))
                .orElseGet(() -> resolver.withoutProfile(prank));
    }

    /**
     * The emblems of the player with this Minecraft name, who may not be online: what is known of their account, their prank level
     * and the emblems they chose. A name that is not on the list, or an account the API has told nothing of, has the first prank level
     * and nothing chosen.
     */
    public PlayerEmblems emblemsOf(String name) {
        EmblemSelections selections = read.get();
        Optional<Integer> account = linked.byName(name).map(player -> player.userId());
        Optional<String> prank = account.flatMap(selections::prankOf).map(EmblemSelections.Prank::level);
        return account.flatMap(cache::byAccount)
                .map(profile -> resolver.resolve(profile, selections.of(profile.userId()), prank))
                .orElseGet(() -> resolver.withoutProfile(prank));
    }

    /** The Panita account of a player, if they are on the list. */
    public Optional<Integer> accountOf(ServerPlayer player) {
        return linked.byName(player.getGameProfile().name()).map(account -> account.userId());
    }

    /** Every prank level, lowest first; the first is where players start and cannot be chosen. */
    public List<EmblemView> prankLevels() {
        return resolver.levels();
    }

    /** Remembers which emblems the player wants shown, in order. Ids that are not selectable are dropped when read. */
    public void choose(ServerPlayer player, List<String> emblems) {
        accountOf(player).ifPresent(userId -> write.accept(read.get().with(userId, emblems)));
    }

    /** Back to the default: the first emblems the player holds. The prank level is a choice of its own and is kept. */
    public void resetChoice(ServerPlayer player) {
        accountOf(player).ifPresent(userId -> write.accept(read.get().without(userId)));
    }

    /**
     * Changes the prank level of the player, unless the last change was less than the configured wait ago. The first change has no
     * wait. The first level is only where players start: nobody can choose it, and once a player has left it they cannot return,
     * not even with the wait ignored.
     *
     * @param bypass whether the wait is ignored
     */
    public PrankChange choosePrank(ServerPlayer player, String level, boolean bypass) {
        return accountOf(player).map(userId -> choosePrank(userId, level, bypass)).orElseGet(() -> PrankChange.of(PrankChange.Status.NOT_LINKED));
    }

    PrankChange choosePrank(int userId, String level, boolean bypass) {
        EmblemSelections selections = read.get();
        Optional<EmblemSelections.Prank> current = selections.prankOf(userId);
        String shown = current.map(EmblemSelections.Prank::level).filter(resolver::isPrank).orElseGet(resolver::defaultPrankId);
        if (!resolver.isPrank(level)) {
            return PrankChange.of(PrankChange.Status.UNKNOWN);
        }
        if (shown.equals(level)) {
            return PrankChange.of(PrankChange.Status.SAME);
        }
        if (level.equals(resolver.defaultPrankId())) {
            return PrankChange.of(PrankChange.Status.LOCKED);
        }
        if (!bypass && current.isPresent()) {
            Duration remaining = Duration.between(clock.get(), current.get().changedAt().plusSeconds(prankCooldownSeconds.getAsInt()));
            if (remaining.isPositive()) {
                return new PrankChange(PrankChange.Status.COOLDOWN, remaining);
            }
        }
        write.accept(selections.withPrank(userId, level, clock.get()));
        return PrankChange.of(PrankChange.Status.CHANGED);
    }

    /** Called on the server thread, with the player, whenever their data was replaced by a newer read. */
    public void onUpdated(Consumer<ServerPlayer> listener) {
        updates.add(listener);
    }

    /** Tells the listeners the player's data changed. Call it on the server thread. */
    public void updated(ServerPlayer player) {
        updates.forEach(listener -> listener.accept(player));
    }
}
