package com.panita.enriquecraft.link.client;

import com.panita.enriquecraft.auth.data.AuthMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpPanitaApiTest {

    private static final String KEY = "secret-key-value";

    private FakePanitaServer server;
    private Optional<String> key = Optional.of(KEY);
    private Duration timeout = Duration.ofSeconds(5);
    private HttpPanitaApi api;

    @BeforeEach
    void start() throws IOException {
        server = new FakePanitaServer();
        api = new HttpPanitaApi(server::url, () -> timeout, () -> key);
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private PanitaApi.Result ask(Optional<String> etag) {
        return api.players(etag).join();
    }

    @Test
    void readsTheListWithItsEtag() {
        server.body = FakePanitaServer.list(
                FakePanitaServer.player(12, "Javithor360", "premium", "123456789012345678"),
                FakePanitaServer.player(30, "Tezzlar", "cracked", "223456789012345678"));

        PanitaApi.Result result = ask(Optional.empty());

        assertEquals(new PanitaApi.Result.Players(List.of(
                new LinkedPlayer(12, "Javithor360", AuthMode.PREMIUM, "123456789012345678"),
                new LinkedPlayer(30, "Tezzlar", AuthMode.CRACKED, "223456789012345678")), Optional.of("\"v1\"")), result);
    }

    @Test
    void sendsTheKeyAndAsksOnlyForTheListPath() {
        ask(Optional.empty());

        FakePanitaServer.Seen seen = server.requests.get(0);
        assertEquals("/v1/mod/players", seen.path());
        assertEquals(KEY, seen.headers().get("x-api-key"));
        assertFalse(seen.headers().containsKey("if-none-match"));
    }

    @Test
    void asksWhetherTheVersionItHasIsStillCurrent() {
        server.status = 304;

        PanitaApi.Result result = ask(Optional.of("\"v1\""));

        assertInstanceOf(PanitaApi.Result.NotModified.class, result);
        assertEquals("\"v1\"", server.requests.get(0).headers().get("if-none-match"));
    }

    @Test
    void aRefusedKeyIsAFailureThatDoesNotRepeatTheKey() {
        server.status = 401;

        PanitaApi.Result.Failed failed = assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));

        assertTrue(failed.reason().contains("401"));
        assertFalse(failed.reason().contains(KEY));
    }

    @Test
    void aServerErrorIsAFailure() {
        server.status = 500;

        assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));
    }

    @Test
    void aBodyThatIsNotTheDocumentedListIsAFailure() {
        List<String> bodies = List.of("not json", "{}", "{\"data\":{}}", "{\"data\":[1]}",
                // a name nobody can have, an unknown status, a missing field
                FakePanitaServer.list(FakePanitaServer.player(1, "bad name", "premium", "1")),
                FakePanitaServer.list(FakePanitaServer.player(1, "Javi", "maybe", "1")),
                FakePanitaServer.list("{\"user_id\":1,\"ign\":\"Javi\",\"minecraft_status\":\"premium\"}"));
        for (String body : bodies) {
            server.body = body;

            assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()), body);
        }
    }

    @Test
    void oneBadEntryFailsTheWholeListSoNobodyIsRemovedByHalfAnAnswer() {
        server.body = FakePanitaServer.list(
                FakePanitaServer.player(1, "Javi", "premium", "1"),
                FakePanitaServer.player(2, "bad name", "premium", "2"));

        assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));
    }

    @Test
    void aSlowServerTimesOut() {
        server.delayMillis = 1500;
        timeout = Duration.ofMillis(200);

        assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));
    }

    @Test
    void withoutAKeyNothingIsSent() {
        key = Optional.empty();

        assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void anUnreachableServerIsAFailure() {
        server.close();

        assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));
    }

    @Test
    void anAddressThatIsNotValidIsAFailure() {
        api = new HttpPanitaApi(() -> "not a url", () -> timeout, () -> key);

        assertInstanceOf(PanitaApi.Result.Failed.class, ask(Optional.empty()));
    }
}
