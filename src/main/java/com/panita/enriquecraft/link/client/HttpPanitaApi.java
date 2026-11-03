package com.panita.enriquecraft.link.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.util.StringUtil;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Reads the list of players over HTTP with the JDK's own client. The request, with the address, the timeout and the key, is
 * made by {@link ApiAccess}.
 */
public final class HttpPanitaApi implements PanitaApi {

    private static final String PATH = "/v1/mod/players";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ApiAccess access;

    public HttpPanitaApi(ApiAccess access) {
        this.access = access;
    }

    public HttpPanitaApi(Supplier<String> baseUrl, Supplier<Duration> timeout, Supplier<Optional<String>> key) {
        this(new ApiAccess(baseUrl, timeout, key));
    }

    @Override
    public CompletableFuture<Result> players(Optional<String> etag) {
        return switch (access.get(PATH, etag)) {
            case ApiAccess.Prepared.Unavailable unavailable -> CompletableFuture.completedFuture(new Result.Failed(unavailable.reason()));
            case ApiAccess.Prepared.Ready ready -> client.sendAsync(ready.request(), HttpResponse.BodyHandlers.ofString())
                    .<Result>thenApply(response -> read(response.statusCode(), response.headers().firstValue("ETag"), response.body()))
                    .exceptionally(error -> new Result.Failed("the API could not be reached (" + error.getClass().getSimpleName() + ")"));
        };
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
