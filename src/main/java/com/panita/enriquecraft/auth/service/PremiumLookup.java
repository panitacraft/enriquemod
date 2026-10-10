package com.panita.enriquecraft.auth.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.players.NameAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Asks Mojang whether a name belongs to a Microsoft account. Only used for names that the whitelist and
 * the op list say nothing about. Answers are remembered for a while, the number of questions is kept
 * below what Mojang allows, and when the answer cannot be had the result is {@link Result.Unavailable}:
 * callers must then refuse, because treating an unknown name as having no account would let anyone take
 * a premium name.
 */
public final class PremiumLookup {

    public sealed interface Result {
        /** A Microsoft account has this name; the profile carries its real UUID and exact spelling. */
        record Found(NameAndId profile) implements Result {
        }

        /** Mojang answered that no account has this name. */
        record Missing() implements Result {
        }

        /** Mojang could not be asked or did not give a usable answer. */
        record Unavailable() implements Result {
        }
    }

    /** Where answers come from. */
    @FunctionalInterface
    public interface Fetcher {
        CompletableFuture<Result> fetch(String name);
    }

    private record Cached(Result result, Instant expiresAt) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PremiumLookup.class);
    private static final String ENDPOINT = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final Duration TIMEOUT = Duration.ofSeconds(4);
    private static final Duration BUDGET_WINDOW = Duration.ofMinutes(10);

    private final Fetcher fetcher;
    private final Supplier<Instant> clock;
    private final Supplier<Duration> cacheFor;
    private final IntSupplier maxRequests;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Deque<Instant> requests = new ArrayDeque<>();

    public PremiumLookup(Fetcher fetcher, Supplier<Instant> clock, Supplier<Duration> cacheFor, IntSupplier maxRequests) {
        this.fetcher = fetcher;
        this.clock = clock;
        this.cacheFor = cacheFor;
        this.maxRequests = maxRequests;
    }

    /** The lookup that really asks Mojang, over HTTPS. */
    public static Fetcher mojang() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        return name -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + name))
                    .timeout(TIMEOUT).header("Accept", "application/json").GET().build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> parse(response.statusCode(), response.body()));
        };
    }

    /**
     * @param name a valid player name; the caller has already checked it, which is what keeps it safe to put in a URL
     */
    public CompletableFuture<Result> lookup(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Cached cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(clock.get())) {
            return CompletableFuture.completedFuture(cached.result());
        }
        if (!takeRequest()) {
            LOGGER.warn("The limit of questions to Mojang is reached; refusing to guess about {}", name);
            return CompletableFuture.completedFuture(new Result.Unavailable());
        }
        return fetcher.fetch(name)
                .exceptionally(error -> {
                    LOGGER.warn("Could not ask Mojang about {}: {}", name, error.toString());
                    return new Result.Unavailable();
                })
                .thenApply(result -> {
                    if (!(result instanceof Result.Unavailable)) {
                        cache.put(key, new Cached(result, clock.get().plus(cacheFor.get())));
                    }
                    return result;
                });
    }

    private synchronized boolean takeRequest() {
        Instant now = clock.get();
        while (!requests.isEmpty() && requests.peekFirst().plus(BUDGET_WINDOW).isBefore(now)) {
            requests.removeFirst();
        }
        if (requests.size() >= maxRequests.getAsInt()) {
            return false;
        }
        requests.addLast(now);
        return true;
    }

    /** Reads an answer of Mojang's name lookup. */
    static Result parse(int status, String body) {
        if (status == 404 || status == 204) {
            return new Result.Missing();
        }
        if (status != 200) {
            return new Result.Unavailable();
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return new Result.Found(new NameAndId(parseUndashed(json.get("id").getAsString()), json.get("name").getAsString()));
        } catch (RuntimeException e) {
            return new Result.Unavailable();
        }
    }

    private static UUID parseUndashed(String id) {
        if (id.length() != 32) {
            throw new IllegalArgumentException("Not an undashed UUID: " + id);
        }
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-"
                + id.substring(16, 20) + "-" + id.substring(20));
    }
}
