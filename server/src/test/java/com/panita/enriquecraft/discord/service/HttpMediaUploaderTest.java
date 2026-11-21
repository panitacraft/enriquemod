package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpMediaUploaderTest {

    private static final String ANSWER = "{\"url\":\"https://cdn.discordapp.com/attachments/1/2/captura.jpg\",\"type\":\"image/jpeg\","
            + "\"size\":1234,\"width\":640,\"height\":360,\"messageId\":\"99\"}";

    private HttpServer server;
    private ExecutorService executor;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String answer = ANSWER;

    @BeforeEach
    void start() throws IOException {
        executor = Executors.newFixedThreadPool(2);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bridge/media", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private HttpMediaUploader uploader(Optional<String> key) {
        return new HttpMediaUploader(executor, () -> "ws://127.0.0.1:" + server.getAddress().getPort() + "/bridge", () -> key);
    }

    private static MediaUploader.Request request() {
        PlayerInfo player = new PlayerInfo(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Tez", Optional.empty(), true, Optional.empty());
        return new MediaUploader.Request(player, "global", "mi base", new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2});
    }

    @Test
    void sendsTheKeyTheMetadataAndTheFileAndReadsTheAnswer() throws Exception {
        MediaUploader.Uploaded uploaded = uploader(Optional.of("secret")).upload(request()).get();
        assertEquals("Bearer secret", authorization.get());
        assertTrue(body.get().contains("name=\"meta\""));
        assertTrue(body.get().contains("\"channel\":\"global\""));
        assertTrue(body.get().contains("\"caption\":\"mi base\""));
        assertTrue(body.get().contains("\"name\":\"Tez\""));
        assertTrue(body.get().contains("name=\"file\"; filename=\"captura.jpg\""));
        assertEquals("https://cdn.discordapp.com/attachments/1/2/captura.jpg", uploaded.url());
        assertEquals(640, uploaded.width());
        assertEquals("99", uploaded.messageId());
    }

    private MediaUploader.Failure failureOf(MediaUploader.Request request, Optional<String> key) {
        CompletionException error = assertThrows(CompletionException.class, () -> uploader(key).upload(request).join());
        return assertInstanceOf(MediaUploader.UploadException.class, error.getCause()).failure();
    }

    @Test
    void theCodesOfTheBotBecomeFailures() {
        status = 429;
        answer = "{\"error\":\"rate\"}";
        assertEquals(MediaUploader.Failure.RATE, failureOf(request(), Optional.of("k")));
        status = 413;
        answer = "{\"error\":\"too_large\"}";
        assertEquals(MediaUploader.Failure.TOO_LARGE, failureOf(request(), Optional.of("k")));
        status = 401;
        answer = "{\"error\":\"auth\"}";
        assertEquals(MediaUploader.Failure.AUTH, failureOf(request(), Optional.of("k")));
        status = 503;
        answer = "not json";
        assertEquals(MediaUploader.Failure.UNAVAILABLE, failureOf(request(), Optional.of("k")));
    }

    @Test
    void anAnswerThatIsNotTheDocumentedShapeFails() {
        answer = "{\"url\":\"https://cdn.discordapp.com/x.jpg\"}";
        assertEquals(MediaUploader.Failure.UNAVAILABLE, failureOf(request(), Optional.of("k")));
        answer = ANSWER.replace("\"width\":640", "\"width\":0");
        assertEquals(MediaUploader.Failure.UNAVAILABLE, failureOf(request(), Optional.of("k")));
    }

    @Test
    void withoutAKeyNothingIsSent() {
        assertEquals(MediaUploader.Failure.UNAVAILABLE, failureOf(request(), Optional.empty()));
        assertEquals(null, authorization.get());
    }

    @Test
    void theUploadAddressFollowsTheBridge() {
        assertEquals(Optional.of(URI.create("https://bot.example.com/bridge/media")), HttpMediaUploader.endpointOf("wss://bot.example.com/bridge"));
        assertEquals(Optional.of(URI.create("http://localhost:3000/bridge/media")), HttpMediaUploader.endpointOf("ws://localhost:3000/bridge/"));
        assertEquals(Optional.of(URI.create("https://bot.example.com/media")), HttpMediaUploader.endpointOf("wss://bot.example.com"));
        assertEquals(Optional.empty(), HttpMediaUploader.endpointOf("http://bot.example.com/bridge"));
        assertEquals(Optional.empty(), HttpMediaUploader.endpointOf(""));
    }
}
