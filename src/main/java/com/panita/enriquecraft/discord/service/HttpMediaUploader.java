package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.panita.enriquecraft.discord.protocol.FrameCodec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Sends a screenshot to the bot's {@code POST /bridge/media} with the bridge's key, the way the bot's contract describes. The
 * address is the bridge's, on {@code https} (or {@code http} for the local development address); the key is read at each
 * upload and never logged. The answer is read strictly: anything that is not the documented shape is a failure.
 */
public final class HttpMediaUploader implements MediaUploader {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_ANSWER_BYTES = 16 * 1024;

    private final HttpClient client;
    private final Executor executor;
    private final Supplier<String> bridgeUrl;
    private final Supplier<Optional<String>> key;
    private final SecureRandom random = new SecureRandom();

    /** @param bridgeUrl the address of the bridge (a {@code wss} or {@code ws} one), read at each upload */
    public HttpMediaUploader(Executor executor, Supplier<String> bridgeUrl, Supplier<Optional<String>> key) {
        this.executor = executor;
        this.bridgeUrl = bridgeUrl;
        this.key = key;
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(TIMEOUT).executor(executor).build();
    }

    /** The upload address that goes with the address of the bridge: the same host, over http, with {@code /media} added. */
    static Optional<URI> endpointOf(String bridge) {
        try {
            URI uri = new URI(bridge);
            String scheme = switch (uri.getScheme() == null ? "" : uri.getScheme()) {
                case "wss" -> "https";
                case "ws" -> "http";
                default -> null;
            };
            if (scheme == null || uri.getRawAuthority() == null) {
                return Optional.empty();
            }
            String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
            return Optional.of(new URI(scheme, uri.getRawAuthority(), null, null, null).resolve(path + "/media"));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Uploaded> upload(Request request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(request);
            } catch (UploadException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private Uploaded send(Request request) throws UploadException {
        Optional<URI> endpoint = endpointOf(bridgeUrl.get());
        Optional<String> secret = key.get();
        if (endpoint.isEmpty() || secret.isEmpty()) {
            throw new UploadException(Failure.UNAVAILABLE, null);
        }
        String boundary = "enriquecraft" + HexFormat.of().formatHex(randomBytes());
        HttpRequest http = HttpRequest.newBuilder(endpoint.get()).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + secret.get())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body(boundary, request))).build();
        try {
            HttpResponse<byte[]> response = client.send(http, HttpResponse.BodyHandlers.ofByteArray());
            if (response.body().length > MAX_ANSWER_BYTES) {
                throw new UploadException(Failure.UNAVAILABLE, null);
            }
            String text = new String(response.body(), StandardCharsets.UTF_8);
            if (response.statusCode() == 200) {
                return answer(text);
            }
            throw new UploadException(failureOf(response.statusCode(), text), null);
        } catch (IOException e) {
            throw new UploadException(Failure.UNAVAILABLE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UploadException(Failure.UNAVAILABLE, e);
        }
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return bytes;
    }

    private static byte[] body(String boundary, Request request) {
        JsonObject meta = new JsonObject();
        meta.add("player", FrameCodec.player(request.player()));
        meta.addProperty("channel", request.channel());
        meta.addProperty("caption", request.caption());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"meta\"\r\nContent-Type: application/json\r\n\r\n");
        write(out, meta.toString());
        write(out, "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"captura.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n");
        out.writeBytes(request.jpeg());
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private static Failure failureOf(int status, String text) {
        String code = "";
        try {
            JsonElement error = JsonParser.parseString(text).getAsJsonObject().get("error");
            code = error != null && error.isJsonPrimitive() ? error.getAsString() : "";
        } catch (RuntimeException ignored) {
            // The status alone decides
        }
        return switch (code.isEmpty() ? String.valueOf(status) : code) {
            case "bad_request", "400" -> Failure.BAD_REQUEST;
            case "auth", "401" -> Failure.AUTH;
            case "channel", "404" -> Failure.CHANNEL;
            case "too_large", "413" -> Failure.TOO_LARGE;
            case "type", "415" -> Failure.TYPE;
            case "rate", "429" -> Failure.RATE;
            default -> Failure.UNAVAILABLE;
        };
    }

    private static Uploaded answer(String text) throws UploadException {
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String url = json.getAsJsonPrimitive("url").getAsString();
            String type = json.getAsJsonPrimitive("type").getAsString();
            long size = json.getAsJsonPrimitive("size").getAsLong();
            int width = json.getAsJsonPrimitive("width").getAsInt();
            int height = json.getAsJsonPrimitive("height").getAsInt();
            String messageId = json.getAsJsonPrimitive("messageId").getAsString();
            if (url.isEmpty() || width <= 0 || height <= 0 || size <= 0) {
                throw new UploadException(Failure.UNAVAILABLE, null);
            }
            return new Uploaded(url, type, size, width, height, messageId);
        } catch (RuntimeException e) {
            throw new UploadException(Failure.UNAVAILABLE, e);
        }
    }
}
