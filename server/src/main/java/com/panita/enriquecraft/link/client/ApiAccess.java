package com.panita.enriquecraft.link.client;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * How the server reaches the Panita API: the address, the timeout and the service key, each read at the moment of a request so
 * a reload of the config or a key put in place later is picked up. Every module that asks the API builds its requests here, so
 * the key is only ever put in a header, in one place, and never appears in a log or a failure reason.
 */
public final class ApiAccess {

    /** A request ready to send, or why none can be made. */
    public sealed interface Prepared {
        record Ready(HttpRequest request) implements Prepared {
        }

        record Unavailable(String reason) implements Prepared {
        }
    }

    private final Supplier<String> baseUrl;
    private final Supplier<Duration> timeout;
    private final Supplier<Optional<String>> key;

    public ApiAccess(Supplier<String> baseUrl, Supplier<Duration> timeout, Supplier<Optional<String>> key) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.key = key;
    }

    public boolean hasKey() {
        return key.get().isPresent();
    }

    public Duration timeout() {
        return timeout.get();
    }

    /**
     * A {@code GET} of a path of the API, with the key, as JSON.
     *
     * @param etag the tag of the version the caller has, to be told when it did not change
     */
    public Prepared get(String path, Optional<String> etag) {
        Optional<String> apiKey = key.get();
        if (apiKey.isEmpty()) {
            return new Prepared.Unavailable("there is no API key");
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl.get() + path))
                    .timeout(timeout.get()).header("Accept", "application/json").header("X-Api-Key", apiKey.get()).GET();
            etag.ifPresent(tag -> builder.header("If-None-Match", tag));
            return new Prepared.Ready(builder.build());
        } catch (IllegalArgumentException e) {
            return new Prepared.Unavailable("the API address is not valid");
        }
    }
}
