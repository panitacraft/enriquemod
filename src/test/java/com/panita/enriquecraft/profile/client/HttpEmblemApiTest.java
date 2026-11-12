package com.panita.enriquecraft.profile.client;

import com.panita.enriquecraft.link.client.ApiAccess;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpEmblemApiTest {

    private static final String PROFILE = """
            {"data": {"user_id": 7, "ign": "Alex", "discord_name": "alex", "avatar_url": null, "joined_at": "2026-01-01T00:00:00.000Z",
              "emblems": [
                {"id": "staff", "name": "Staff", "description": "Del equipo", "icon_url": "https://res.cloudinary.com/x/staff.png",
                 "category": "local_emblem", "position": 1, "edition_id": "enriquecraft6"},
                {"id": "nationality_co", "name": "Colombia", "description": null, "icon_url": "https://res.cloudinary.com/x/co.png",
                 "category": "system", "position": 2, "edition_id": null}
              ]}}
            """;

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> keys = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body = PROFILE;
    private Optional<String> key = Optional.of("secret-key-value");
    private HttpEmblemApi api;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI().toString());
            keys.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-Api-Key")));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        api = new HttpEmblemApi(new ApiAccess(() -> url, () -> Duration.ofSeconds(5), () -> key));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private EmblemApi.Result ask() {
        return api.profile(7, "enriquecraft6").join();
    }

    @Test
    void readsTheProfileAndItsEmblems() {
        EmblemApi.Result.Found found = assertInstanceOf(EmblemApi.Result.Found.class, ask());

        assertEquals(7, found.profile().userId());
        assertEquals(Optional.of("Alex"), found.profile().ign());
        assertEquals("alex", found.profile().discordName());
        assertEquals(2, found.profile().emblems().size());
        ApiEmblem staff = found.profile().emblems().get(0);
        assertEquals(new ApiEmblem("staff", "Staff", Optional.of("Del equipo"), Optional.of("https://res.cloudinary.com/x/staff.png"),
                EmblemCategory.LOCAL_EMBLEM, 1, Optional.of("enriquecraft6")), staff);
        assertEquals(Optional.empty(), found.profile().emblems().get(1).description());
        assertEquals(EmblemCategory.SYSTEM, found.profile().emblems().get(1).category());
    }

    @Test
    void asksForTheEditionOfTheServerWithTheKey() {
        ask();

        assertEquals("/v1/mod/players/7/profile?edition_id=enriquecraft6", requests.get(0));
        assertEquals("secret-key-value", keys.get(0));
    }

    @Test
    void anUnknownPlayerIsNotFound() {
        status = 404;

        assertInstanceOf(EmblemApi.Result.NotFound.class, ask());
    }

    @Test
    void aRefusedKeyAndOtherStatusesAreFailuresThatNameNoSecret() {
        status = 401;
        EmblemApi.Result.Failed refused = assertInstanceOf(EmblemApi.Result.Failed.class, ask());
        status = 500;
        EmblemApi.Result.Failed broken = assertInstanceOf(EmblemApi.Result.Failed.class, ask());

        assertTrue(refused.reason().contains("401"));
        assertTrue(broken.reason().contains("500"));
        assertTrue(!refused.reason().contains("secret-key-value"));
    }

    @Test
    void aProfileThatIsNotTheDocumentedShapeFailsAsAWhole() {
        body = PROFILE.replace("\"local_emblem\"", "\"mystery\"");
        assertInstanceOf(EmblemApi.Result.Failed.class, ask());

        body = "not json";
        assertInstanceOf(EmblemApi.Result.Failed.class, ask());
    }

    @Test
    void withoutAKeyNothingIsSent() {
        key = Optional.empty();

        EmblemApi.Result.Failed failed = assertInstanceOf(EmblemApi.Result.Failed.class, ask());

        assertEquals("there is no API key", failed.reason());
        assertTrue(requests.isEmpty());
    }

    @Test
    void anApiThatIsDownIsAFailure() {
        server.stop(0);

        assertInstanceOf(EmblemApi.Result.Failed.class, ask());
    }
}
