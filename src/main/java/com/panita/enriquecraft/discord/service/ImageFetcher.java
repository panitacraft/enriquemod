package com.panita.enriquecraft.discord.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Downloads one image for a player who asked to see it. The address has already been checked against the allowed hosts; here
 * the answer is limited: no redirects (they could lead to another host), only an image, never more than the limit of bytes,
 * and a time limit. It runs on a worker, never on the server thread.
 */
public final class ImageFetcher {

    public enum Failure {
        TOO_LARGE,
        NOT_AN_IMAGE,
        UNAVAILABLE
    }

    public static final class FetchException extends Exception {
        private final Failure failure;

        FetchException(Failure failure, Throwable cause) {
            super(failure.name(), cause);
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }

    private final HttpClient client;
    private final Executor executor;
    private final int maxBytes;
    private final Duration timeout;

    public ImageFetcher(Executor executor, int maxBytes, Duration timeout) {
        this.executor = executor;
        this.maxBytes = maxBytes;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(timeout).executor(executor).build();
    }

    /** The bytes of the image; a failure is a {@link FetchException}, seen as the cause of the {@link CompletionException}. */
    public CompletableFuture<byte[]> fetch(URI address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return read(address);
            } catch (FetchException e) {
                throw new CompletionException(e);
            }
        }, executor).orTimeout(timeout.toMillis() * 2, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private byte[] read(URI address) throws FetchException {
        HttpRequest request = HttpRequest.newBuilder(address).timeout(timeout).header("Accept", "image/*").GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new FetchException(Failure.UNAVAILABLE, null);
                }
                if (!response.headers().firstValue("content-type").map(type -> type.toLowerCase(java.util.Locale.ROOT).startsWith("image/")).orElse(false)) {
                    throw new FetchException(Failure.NOT_AN_IMAGE, null);
                }
                if (response.headers().firstValueAsLong("content-length").orElse(0) > maxBytes) {
                    throw new FetchException(Failure.TOO_LARGE, null);
                }
                byte[] bytes = body.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw new FetchException(Failure.TOO_LARGE, null);
                }
                return bytes;
            }
        } catch (IOException e) {
            throw new FetchException(Failure.UNAVAILABLE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException(Failure.UNAVAILABLE, e);
        }
    }
}
