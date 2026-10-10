package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Decides, as soon as a player says their name and before anything else is exchanged, whether they may
 * come in and how they prove who they are. The answer comes from the block list of addresses, then the
 * whitelist and the ops, and only for names they say nothing about, from Mojang.
 */
public final class LoginGate {

    public sealed interface Verdict {
        /** The player may continue, proving themselves the given way. */
        record Proceed(AuthMode mode) implements Verdict {
        }

        /** The address failed too many PINs recently. */
        record Blocked(Duration remaining) implements Verdict {
        }

        record NotWhitelisted() implements Verdict {
        }

        /** The name could not be classified; it is refused rather than guessed. */
        record Unavailable() implements Verdict {
        }
    }

    private final AttemptLimiter limiter;
    private final PremiumLookup lookup;

    public LoginGate(AttemptLimiter limiter, PremiumLookup lookup) {
        this.limiter = limiter;
        this.lookup = lookup;
    }

    /**
     * Decides about a player who is connecting. The lists are read on the server thread, since staff change
     * them there; the answer may complete on another thread.
     */
    public CompletableFuture<Verdict> admit(MinecraftServer server, String address, String name) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerList players = server.getPlayerList();
            return plan(limiter.blockedFor(address), players.isUsingWhitelist(),
                    WhitelistModes.listedMode(players, name), name);
        }, server).thenCompose(plan -> plan);
    }

    /** What to do given what is known about the address and the name. */
    CompletableFuture<Verdict> plan(Optional<Duration> blocked, boolean usingWhitelist, Optional<AuthMode> listed,
                                    String name) {
        if (blocked.isPresent()) {
            return CompletableFuture.completedFuture(new Verdict.Blocked(blocked.get()));
        }
        if (listed.isPresent()) {
            return CompletableFuture.completedFuture(new Verdict.Proceed(listed.get()));
        }
        if (usingWhitelist) {
            return CompletableFuture.completedFuture(new Verdict.NotWhitelisted());
        }
        return lookup.lookup(name).thenApply(result -> switch (result) {
            case PremiumLookup.Result.Found found -> new Verdict.Proceed(AuthMode.PREMIUM);
            case PremiumLookup.Result.Missing missing -> new Verdict.Proceed(AuthMode.CRACKED);
            case PremiumLookup.Result.Unavailable unavailable -> new Verdict.Unavailable();
        });
    }
}
