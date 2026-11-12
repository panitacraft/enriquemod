package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What the rest of the mod asks about a player's profile: their emblems as this server shows them, who they are on Panitacraft,
 * and what they chose to show. It reads the cache and the choices; it never waits for the network.
 */
public final class ProfileService {

    private final ProfileCache cache;
    private final EmblemResolver resolver;
    private final LinkedPlayers linked;
    private final Supplier<EmblemSelections> read;
    private final Consumer<EmblemSelections> write;
    private final List<Consumer<ServerPlayer>> updates = new CopyOnWriteArrayList<>();

    public ProfileService(ProfileCache cache, EmblemResolver resolver, LinkedPlayers linked, Supplier<EmblemSelections> read,
                          Consumer<EmblemSelections> write) {
        this.cache = cache;
        this.resolver = resolver;
        this.linked = linked;
        this.read = read;
        this.write = write;
    }

    public Optional<ModProfile> profileOf(UUID player) {
        return cache.get(player);
    }

    /** The emblems of the player as this server shows them; only the default prank level for a player the API has not said anything of. */
    public PlayerEmblems emblemsOf(UUID player) {
        return cache.get(player).map(profile -> {
            EmblemSelections selections = read.get();
            return resolver.resolve(profile, selections.of(profile.userId()), selections.prankOf(profile.userId()));
        }).orElseGet(resolver::withoutProfile);
    }

    /** The Panita account of a player, if they are on the list. */
    public Optional<Integer> accountOf(ServerPlayer player) {
        return linked.byName(player.getGameProfile().name()).map(account -> account.userId());
    }

    /** Remembers which emblems the player wants shown, in order. Ids that are not selectable are dropped when read. */
    public void choose(ServerPlayer player, List<String> emblems) {
        accountOf(player).ifPresent(userId -> write.accept(read.get().with(userId, emblems)));
    }

    /** Back to the default: the first emblems the player holds. The prank level is a choice of its own and is kept. */
    public void resetChoice(ServerPlayer player) {
        accountOf(player).ifPresent(userId -> write.accept(read.get().without(userId)));
    }

    /** Remembers which prank level the player wants shown, or, with none, goes back to the default level. A level they do not hold is ignored when read. */
    public void choosePrank(ServerPlayer player, Optional<String> emblem) {
        accountOf(player).ifPresent(userId -> write.accept(emblem.map(id -> read.get().withPrank(userId, id)).orElseGet(() -> read.get().withoutPrank(userId))));
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
