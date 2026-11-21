package com.panita.enriquecraft.link.client;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A stand-in for the Panita API, a real HTTP server on a free local port, so the client is tested over the wire.
 * It answers {@code GET /v1/mod/players} the way the API documents it, with whatever the test sets.
 */
final class FakePanitaServer implements AutoCloseable {

    /** What the server saw of a request. */
    record Seen(String path, Map<String, String> headers) {
    }

    private final HttpServer server;
    final List<Seen> requests = new CopyOnWriteArrayList<>();
    volatile int status = 200;
    volatile String body = list();
    volatile String etag = "\"v1\"";
    volatile long delayMillis = 0;

    FakePanitaServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
            requests.add(new Seen(exchange.getRequestURI().getPath(), headers));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (status == 200) {
                exchange.getResponseHeaders().add("ETag", etag);
            }
            exchange.sendResponseHeaders(status, status == 304 ? -1 : bytes.length);
            if (status != 304) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** The body of a list with the given entries, as the API writes it. */
    static String list(String... entries) {
        return "{\"data\":[" + String.join(",", entries) + "],\"meta\":{\"count\":" + entries.length + "}}";
    }

    static String player(int userId, String ign, String status, String discordId) {
        return "{\"user_id\":" + userId + ",\"ign\":\"" + ign + "\",\"minecraft_status\":\"" + status
                + "\",\"discord_id\":\"" + discordId + "\"}";
    }
}
