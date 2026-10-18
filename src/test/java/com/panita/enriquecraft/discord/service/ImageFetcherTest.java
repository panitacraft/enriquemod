package com.panita.enriquecraft.discord.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageFetcherTest {

    private static final int LIMIT = 1000;

    private HttpServer server;
    private ExecutorService executor;
    private ImageFetcher fetcher;
    private int okRequests;

    @BeforeEach
    void start() throws IOException {
        executor = Executors.newFixedThreadPool(2);
        fetcher = new ImageFetcher(executor, LIMIT, Duration.ofMillis(800));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            okRequests++;
            reply(exchange, 200, "image/png", new byte[]{1, 2, 3}, true);
        });
        server.createContext("/exact", exchange -> reply(exchange, 200, "image/png", new byte[LIMIT], true));
        server.createContext("/big", exchange -> reply(exchange, 200, "image/png", new byte[LIMIT + 1], true));
        server.createContext("/bigchunked", exchange -> reply(exchange, 200, "image/png", new byte[LIMIT * 20], false));
        server.createContext("/text", exchange -> reply(exchange, 200, "text/html", "<html>".getBytes(), true));
        server.createContext("/missing", exchange -> reply(exchange, 404, "image/png", new byte[]{1}, true));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/ok");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            reply(exchange, 200, "image/png", new byte[]{1}, true);
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String type, byte[] body, boolean withLength) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, withLength ? body.length : 0);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        } catch (IOException e) {
            // The fetcher gave up reading, which is the point of the test
        }
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private ImageFetcher.Failure failureOf(String path) {
        ExecutionException error = assertThrows(ExecutionException.class, () -> fetcher.fetch(url(path)).get(10, TimeUnit.SECONDS));
        return ((ImageFetcher.FetchException) error.getCause()).failure();
    }

    @Test
    void fetchesAnImage() throws Exception {
        assertArrayEquals(new byte[]{1, 2, 3}, fetcher.fetch(url("/ok")).get(10, TimeUnit.SECONDS));
    }

    @Test
    void acceptsAnImageOfExactlyTheLimit() throws Exception {
        assertEquals(LIMIT, fetcher.fetch(url("/exact")).get(10, TimeUnit.SECONDS).length);
    }

    @Test
    void refusesAnImageOverTheLimitWhetherOrNotItSaysHowBigItIs() {
        assertEquals(ImageFetcher.Failure.TOO_LARGE, failureOf("/big"));
        assertEquals(ImageFetcher.Failure.TOO_LARGE, failureOf("/bigchunked"));
    }

    @Test
    void refusesWhatIsNotAnImage() {
        assertEquals(ImageFetcher.Failure.NOT_AN_IMAGE, failureOf("/text"));
    }

    @Test
    void refusesAnErrorStatusAndNeverFollowsARedirect() {
        assertEquals(ImageFetcher.Failure.UNAVAILABLE, failureOf("/missing"));
        assertEquals(ImageFetcher.Failure.UNAVAILABLE, failureOf("/redirect"));
        assertEquals(0, okRequests, "the redirect target was never requested");
    }

    @Test
    void givesUpOnAServerThatTakesTooLong() {
        assertEquals(ImageFetcher.Failure.UNAVAILABLE, failureOf("/slow"));
    }

    @Test
    void anAddressNobodyAnswersIsUnavailable() {
        server.stop(0);

        assertEquals(ImageFetcher.Failure.UNAVAILABLE, failureOf("/ok"));
    }
}
