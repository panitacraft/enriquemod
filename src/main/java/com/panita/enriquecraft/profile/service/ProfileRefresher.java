package com.panita.enriquecraft.profile.service;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * Reads players' profiles again and tells the rest of the mod when they changed: when a player is ready or opens their menu (only
 * if the cache is older than {@code profile.cacheSeconds}), and always when a player asks for their own to be refreshed or staff
 * refresh everyone. It is called and answers on the server thread; the network work
 * happens in the {@link ProfileLoader}.
 */
public final class ProfileRefresher {

    /** How many players the API is asked about at the same time when everyone is refreshed. */
    static final int CONCURRENCY = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileRefresher.class);

    /** How a refresh of one player's own profile ended. */
    public sealed interface SelfResult {
        /** The player refreshed too recently; nothing was asked. */
        record Cooling(long seconds) implements SelfResult {
        }

        record Finished(ProfileLoader.Outcome outcome) implements SelfResult {
        }
    }

    /**
     * How a refresh of everyone ended. A player whose read was already on its way counts as updated, since their data is as new.
     */
    public record Report(int updated, int notLinked, int failed) {

        Report plus(ProfileLoader.Outcome outcome) {
            return switch (outcome) {
                case ProfileLoader.Outcome.Updated updated -> new Report(this.updated + 1, notLinked, failed);
                case ProfileLoader.Outcome.Busy busy -> new Report(this.updated + 1, notLinked, failed);
                case ProfileLoader.Outcome.Current current -> new Report(this.updated + 1, notLinked, failed);
                case ProfileLoader.Outcome.NotLinked notLinked -> new Report(updated, this.notLinked + 1, failed);
                case ProfileLoader.Outcome.Failed failed -> new Report(updated, notLinked, this.failed + 1);
            };
        }
    }

    private final ProfileLoader loader;
    private final ProfileService profiles;
    private final RefreshCooldown cooldown;
    private final IntSupplier cacheSeconds;
    private final AtomicBoolean refreshingAll = new AtomicBoolean();
    private volatile String lastProblem = "";

    public ProfileRefresher(ProfileLoader loader, ProfileService profiles, RefreshCooldown cooldown, IntSupplier cacheSeconds) {
        this.loader = loader;
        this.profiles = profiles;
        this.cooldown = cooldown;
        this.cacheSeconds = cacheSeconds;
    }

    /**
     * Brings one player's profile up to date for a moment that is not a request for the newest data (they joined, they opened
     * their menu): the API is asked only when the cache is older than {@code profile.cacheSeconds}.
     */
    public CompletableFuture<ProfileLoader.Outcome> refresh(ServerPlayer player) {
        return read(player, new ProfileLoader.Freshness.AllowCached(Duration.ofSeconds(cacheSeconds.getAsInt())));
    }

    /** Reads one player's profile and tells the listeners when it changed. The future never fails. */
    private CompletableFuture<ProfileLoader.Outcome> read(ServerPlayer player, ProfileLoader.Freshness freshness) {
        MinecraftServer server = player.level().getServer();
        UUID id = player.getUUID();
        return loader.load(id, player.getGameProfile().name(), freshness).thenCompose(outcome -> {
            CompletableFuture<ProfileLoader.Outcome> done = new CompletableFuture<>();
            server.execute(() -> {
                boolean changed = outcome instanceof ProfileLoader.Outcome.Updated || outcome instanceof ProfileLoader.Outcome.NotLinked;
                if (changed && server.getPlayerList().getPlayer(id) != null) {
                    profiles.updated(player);
                }
                logProblem(outcome);
                done.complete(outcome);
            });
            return done;
        });
    }

    /** The player refreshing their own profile: only when their wait is over. */
    public CompletableFuture<SelfResult> refreshSelf(ServerPlayer player) {
        long remaining = cooldown.remainingSeconds(player.getUUID());
        if (remaining > 0) {
            return CompletableFuture.completedFuture(new SelfResult.Cooling(remaining));
        }
        cooldown.start(player.getUUID());
        return read(player, new ProfileLoader.Freshness.Force()).thenApply(SelfResult.Finished::new);
    }

    /**
     * Refreshes every player online, a few at a time. Empty when a refresh of everyone is already running.
     */
    public CompletableFuture<Optional<Report>> refreshAll(MinecraftServer server) {
        if (!refreshingAll.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        List<ServerPlayer> players = List.copyOf(server.getPlayerList().getPlayers());
        CompletableFuture<Report> chain = CompletableFuture.completedFuture(new Report(0, 0, 0));
        for (int start = 0; start < players.size(); start += CONCURRENCY) {
            List<ServerPlayer> batch = players.subList(start, Math.min(players.size(), start + CONCURRENCY));
            chain = chain.thenCompose(report -> refreshBatch(batch, report));
        }
        return chain.<Optional<Report>>thenApply(Optional::of).whenComplete((report, error) -> refreshingAll.set(false));
    }

    private CompletableFuture<Report> refreshBatch(List<ServerPlayer> batch, Report soFar) {
        List<CompletableFuture<ProfileLoader.Outcome>> reads = new ArrayList<>();
        for (ServerPlayer player : batch) {
            reads.add(read(player, new ProfileLoader.Freshness.Force()));
        }
        return CompletableFuture.allOf(reads.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            Report report = soFar;
            for (CompletableFuture<ProfileLoader.Outcome> read : reads) {
                report = report.plus(read.join());
            }
            return report;
        });
    }

    /** Warns the first time a problem shows up, and again only when it is a different one; repeats go to the debug log. */
    private void logProblem(ProfileLoader.Outcome outcome) {
        if (outcome instanceof ProfileLoader.Outcome.Failed failed) {
            if (failed.reason().equals(lastProblem)) {
                LOGGER.debug("Could not read a player's profile from the Panita API: {}", failed.reason());
            } else {
                LOGGER.warn("Could not read a player's profile from the Panita API: {}", failed.reason());
                lastProblem = failed.reason();
            }
        } else if (outcome instanceof ProfileLoader.Outcome.Updated) {
            lastProblem = "";
        }
    }
}
