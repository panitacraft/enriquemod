package com.panita.enriquecraft.link.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.util.StringUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Reads the list of players over HTTP with the JDK's own client. The address and the timeout are read for each
 * request, so a reload of the config takes effect at once; the key is only ever sent in a header and never
 * appears in a log or a failure reason.
 */
public final class HttpPanitaApi implements PanitaApi {

    private static final String PATH = "/v1/mod/players";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Supplier<String> baseUrl;
    private final Supplier<Duration> timeout;
    private final Supplier<Optional<String>> key;

    public HttpPanitaApi(Supplier<String> baseUrl, Supplier<Duration> timeout, Supplier<Optional<String>> key) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.key = key;
    }

    @Override
    public CompletableFuture<Result> players(Optional<String> etag) {
        Optional<String> apiKey = key.get();
        if (apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(new Result.Failed("there is no API key"));
        }
        HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl.get() + PATH))
                    .timeout(timeout.get()).header("Accept", "application/json").header("X-Api-Key", apiKey.get()).GET();
            etag.ifPresent(tag -> builder.header("If-None-Match", tag));
            request = builder.build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(new Result.Failed("the API address is not valid"));
        }
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Result>thenApply(response -> read(response.statusCode(), response.headers().firstValue("ETag"), response.body()))
                .exceptionally(error -> new Result.Failed("the API could not be reached (" + error.getClass().getSimpleName() + ")"));
    }

    /** Turns an answer into a result. Anything that is not exactly the documented list is a failure. */
    static Result read(int status, Optional<String> etag, String body) {
        return switch (status) {
            case 200 -> parse(etag, body);
            case 304 -> new Result.NotModified();
            case 401, 403 -> new Result.Failed("the API refused the key (status " + status + ")");
            default -> new Result.Failed("the API answered with status " + status);
        };
    }

    private static Result parse(Optional<String> etag, String body) {
        try {
            JsonArray data = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("data");
            List<LinkedPlayer> players = new ArrayList<>();
            for (JsonElement item : data) {
                Optional<LinkedPlayer> player = player(item.getAsJsonObject());
                if (player.isEmpty()) {
                    return new Result.Failed("the list has an entry that is not valid");
                }
                players.add(player.get());
            }
            return new Result.Players(List.copyOf(players), etag);
        } catch (RuntimeException e) {
            return new Result.Failed("the list is not in the expected format");
        }
    }

    private static Optional<LinkedPlayer> player(JsonObject item) {
        int userId = item.get("user_id").getAsInt();
        String ign = item.get("ign").getAsString();
        String discordId = item.get("discord_id").getAsString();
        Optional<AuthMode> mode = AuthMode.CODEC.parse(JsonOps.INSTANCE, item.get("minecraft_status"))
                .result();
        if (!StringUtil.isValidPlayerName(ign) || mode.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LinkedPlayer(userId, ign, mode.get(), discordId));
    }
}
