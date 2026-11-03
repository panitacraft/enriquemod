package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.EmblemApi;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Reads one player's profile from the Panita API into the cache. It knows nothing of the game's threads: the answer arrives on a
 * network thread and the cache is safe to fill from there. A player is only read once at a time.
 */
public final class ProfileLoader {

    /** How a read ended. */
    public sealed interface Outcome {
        /** The cache has the player's current profile. */
        record Updated() implements Outcome {
        }

        /** The player is not on the Panita list, or the API has no active account for them, so they have no emblems. */
        record NotLinked() implements Outcome {
        }

        /** The API could not be asked or did not answer well; the cache keeps what it had. The reason is for the console. */
        record Failed(String reason) implements Outcome {
        }

        /** A read of this player is already on its way. */
        record Busy() implements Outcome {
        }
    }

    private final EmblemApi api;
    private final LinkedPlayers linked;
    private final ProfileCache cache;
    private final Supplier<String> editionId;
    private final Set<UUID> running = ConcurrentHashMap.newKeySet();

    public ProfileLoader(EmblemApi api, LinkedPlayers linked, ProfileCache cache, Supplier<String> editionId) {
        this.api = api;
        this.linked = linked;
        this.cache = cache;
        this.editionId = editionId;
    }

    /**
     * @param name the Minecraft name the player is on the Panita list with
     */
    public CompletableFuture<Outcome> load(UUID player, String name) {
        if (!running.add(player)) {
            return CompletableFuture.completedFuture(new Outcome.Busy());
        }
        Optional<LinkedPlayer> account = linked.byName(name);
        if (account.isEmpty()) {
            cache.remove(player);
            running.remove(player);
            return CompletableFuture.completedFuture(new Outcome.NotLinked());
        }
        return api.profile(account.get().userId(), editionId.get())
                .<Outcome>thenApply(result -> answer(player, result))
                .exceptionally(error -> new Outcome.Failed("unexpected error ("
                        + (error.getCause() != null ? error.getCause() : error).getClass().getSimpleName() + ")"))
                .whenComplete((outcome, error) -> running.remove(player));
    }

    private Outcome answer(UUID player, EmblemApi.Result result) {
        return switch (result) {
            case EmblemApi.Result.Found found -> {
                cache.put(player, found.profile());
                yield new Outcome.Updated();
            }
            case EmblemApi.Result.NotFound notFound -> {
                cache.remove(player);
                yield new Outcome.NotLinked();
            }
            case EmblemApi.Result.Failed failed -> new Outcome.Failed(failed.reason());
        };
    }
}
