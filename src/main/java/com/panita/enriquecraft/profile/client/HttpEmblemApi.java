package com.panita.enriquecraft.profile.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.panita.enriquecraft.link.client.ApiAccess;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Reads a player's profile over HTTP with the JDK's own client. The request, with the address, the timeout and the key, is made
 * by {@link ApiAccess}. An answer that is not exactly the documented shape is a failure as a whole, so half an answer never
 * takes an emblem away from a player.
 */
public final class HttpEmblemApi implements EmblemApi {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ApiAccess access;

    public HttpEmblemApi(ApiAccess access) {
        this.access = access;
    }

    @Override
    public CompletableFuture<Result> profile(int userId, String editionId) {
        String path = "/v1/mod/players/" + userId + "/profile?edition_id=" + URLEncoder.encode(editionId, StandardCharsets.UTF_8);
        return switch (access.get(path, Optional.empty())) {
            case ApiAccess.Prepared.Unavailable unavailable -> CompletableFuture.completedFuture(new Result.Failed(unavailable.reason()));
            case ApiAccess.Prepared.Ready ready -> client.sendAsync(ready.request(), HttpResponse.BodyHandlers.ofString())
                    .<Result>thenApply(response -> read(response.statusCode(), response.body()))
                    .exceptionally(error -> new Result.Failed("the API could not be reached (" + error.getClass().getSimpleName() + ")"));
        };
    }

    /** Turns an answer into a result. */
    static Result read(int status, String body) {
        return switch (status) {
            case 200 -> parse(body);
            case 404 -> new Result.NotFound();
            case 401, 403 -> new Result.Failed("the API refused the key (status " + status + ")");
            default -> new Result.Failed("the API answered with status " + status);
        };
    }

    private static Result parse(String body) {
        try {
            JsonObject data = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("data");
            List<ApiEmblem> emblems = new ArrayList<>();
            JsonArray items = data.getAsJsonArray("emblems");
            for (JsonElement item : items) {
                Optional<ApiEmblem> emblem = emblem(item.getAsJsonObject());
                if (emblem.isEmpty()) {
                    return new Result.Failed("the profile has an emblem that is not valid");
                }
                emblems.add(emblem.get());
            }
            return new Result.Found(new ModProfile(data.get("user_id").getAsInt(), text(data, "ign"),
                    data.get("discord_name").getAsString(), emblems));
        } catch (RuntimeException e) {
            return new Result.Failed("the profile is not in the expected format");
        }
    }

    private static Optional<ApiEmblem> emblem(JsonObject item) {
        Optional<EmblemCategory> category = EmblemCategory.byId(item.get("category").getAsString());
        String id = item.get("id").getAsString();
        String name = item.get("name").getAsString();
        if (category.isEmpty() || id.isBlank() || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ApiEmblem(id, name, text(item, "description").filter(description -> !description.isBlank()),
                text(item, "icon_url"), category.get(), item.get("position").getAsInt(), text(item, "edition_id")));
    }

    private static Optional<String> text(JsonObject json, String field) {
        JsonElement element = json.get(field);
        return element == null || element.isJsonNull() ? Optional.empty() : Optional.of(element.getAsString());
    }
}
