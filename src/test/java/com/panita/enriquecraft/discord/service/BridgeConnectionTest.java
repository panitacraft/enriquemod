package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.panita.enriquecraft.discord.protocol.EventKind;
import com.panita.enriquecraft.discord.protocol.InFrame;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import com.panita.enriquecraft.discord.protocol.QueryKind;
import com.panita.enriquecraft.discord.transport.BridgeTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeConnectionTest {

    private static final OutFrame.Hello HELLO = new OutFrame.Hello(1, "0.0.1", "26.3", Instant.parse("2026-10-12T10:00:00Z"), 20,
            Optional.empty(), List.of());
    private static final PlayerInfo JAVI = new PlayerInfo(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360", Optional.empty(), true, Optional.empty());
    private static final String WELCOME = "{\"type\":\"welcome\",\"protocol\":1,\"channels\":[\"global\"]}";

    private static final class FakeTransport implements BridgeTransport {
        record Attempt(URI uri, String key, Listener listener) {
        }

        final List<Attempt> attempts = new ArrayList<>();
        final List<String> sent = new ArrayList<>();
        int closed;
        boolean closedWhileSending;
        CompletableFuture<Void> connectResult = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> sendResult = CompletableFuture.completedFuture(null);

        @Override
        public CompletableFuture<Void> connect(URI uri, String key, Listener listener) {
            attempts.add(new Attempt(uri, key, listener));
            return connectResult;
        }

        @Override
        public CompletableFuture<Void> send(String text) {
            sent.add(text);
            return sendResult;
        }

        @Override
        public void close() {
            closed++;
            closedWhileSending |= !sendResult.isDone();
        }

        Listener listener() {
            return attempts.get(attempts.size() - 1).listener();
        }

        List<String> types() {
            return sent.stream().map(text -> JsonParser.parseString(text).getAsJsonObject().get("type").getAsString()).toList();
        }
    }

    private static final class ManualScheduler implements Scheduler {
        record Task(Duration delay, Runnable task, boolean[] cancelled) {
        }

        final List<Task> tasks = new ArrayList<>();

        @Override
        public Handle schedule(Duration delay, Runnable task) {
            boolean[] cancelled = new boolean[1];
            tasks.add(new Task(delay, task, cancelled));
            return () -> cancelled[0] = true;
        }

        /** Runs the oldest task that was not cancelled, and returns how long it waited. */
        Duration runNext() {
            while (!tasks.isEmpty()) {
                Task next = tasks.remove(0);
                if (!next.cancelled()[0]) {
                    next.task().run();
                    return next.delay();
                }
            }
            throw new AssertionError("nothing was scheduled");
        }

        long pending() {
            return tasks.stream().filter(task -> !task.cancelled()[0]).count();
        }
    }

    private final FakeTransport transport = new FakeTransport();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final List<InFrame> frames = new ArrayList<>();
    private Instant now = Instant.parse("2026-10-12T10:00:00Z");
    private boolean enabled = true;
    private String url = "wss://bot.test/bridge";
    private Optional<String> key = Optional.of("secret");
    private Optional<OutFrame.Hello> hello = Optional.of(HELLO);

    private final BridgeConnection connection = new BridgeConnection(transport, scheduler, () -> now, () -> 0.5, () -> enabled, () -> url,
            () -> key, () -> hello, frames::add);

    private void connected() {
        connection.start();
        transport.listener().onOpen();
        transport.listener().onText(WELCOME);
    }

    @Test
    void doesNothingWithoutTheSettingsItNeeds() {
        enabled = false;
        connection.start();
        assertEquals(BridgeConnection.State.OFF, connection.status().state());

        enabled = true;
        url = "";
        connection.start();
        key = Optional.empty();
        url = "wss://bot.test/bridge";
        connection.start();
        key = Optional.of("secret");
        hello = Optional.empty();
        connection.start();

        assertTrue(transport.attempts.isEmpty());
        assertEquals(BridgeConnection.State.OFF, connection.status().state());
    }

    @Test
    void connectsWithTheKeyAndSaysHelloWhenTheLineOpens() {
        connection.start();

        assertEquals(URI.create("wss://bot.test/bridge"), transport.attempts.get(0).uri());
        assertEquals("secret", transport.attempts.get(0).key());
        assertEquals(BridgeConnection.State.CONNECTING, connection.status().state());

        transport.listener().onOpen();

        assertEquals(List.of("hello"), transport.types());
    }

    @Test
    void isConnectedOnlyOnceTheBotWelcomesIt() {
        connection.start();
        transport.listener().onOpen();
        assertFalse(connection.connected());

        transport.listener().onText(WELCOME);

        BridgeConnection.Status status = connection.status();
        assertEquals(BridgeConnection.State.CONNECTED, status.state());
        assertEquals(List.of("global"), status.channels());
        assertEquals(Optional.of(now), status.since());
        assertTrue(connection.bridges("global"));
        assertFalse(connection.bridges("staff"));
    }

    @Test
    void startingAgainWhileItIsUpDoesNotConnectTwice() {
        connected();

        connection.start();

        assertEquals(1, transport.attempts.size());
    }

    @Test
    void comesBackByItselfAfterAFailureWaitingLongerEachTime() {
        transport.connectResult = CompletableFuture.failedFuture(new IOException("down"));
        connection.start();
        assertEquals(BridgeConnection.State.WAITING, connection.status().state());

        assertEquals(Backoff.delay(0, 0.5), scheduler.runNext());
        assertEquals(Backoff.delay(1, 0.5), scheduler.runNext());
        assertEquals(Backoff.delay(2, 0.5), scheduler.runNext());

        assertEquals(4, transport.attempts.size());
    }

    @Test
    void forgetsTheFailuresOnceItIsWelcomedAgain() {
        transport.connectResult = CompletableFuture.failedFuture(new IOException("down"));
        connection.start();
        scheduler.runNext();
        transport.connectResult = CompletableFuture.completedFuture(null);
        scheduler.runNext();
        transport.listener().onOpen();
        transport.listener().onText(WELCOME);

        transport.listener().onClose(1006, "gone");

        assertEquals(BridgeConnection.State.WAITING, connection.status().state());
        assertEquals(Backoff.delay(0, 0.5), scheduler.tasks.get(scheduler.tasks.size() - 1).delay());
    }

    @Test
    void sayingWhatWentWrongSeesThroughTheWrappersOfAsyncFailures() {
        assertEquals("IOException", BridgeConnection.describe(new java.util.concurrent.CompletionException(new IOException("down"))));
        assertEquals("IllegalStateException", BridgeConnection.describe(new IllegalStateException("x")));
    }

    @Test
    void goesWaitingWhenTheBotClosesTheLine() {
        connected();

        transport.listener().onClose(1001, "bye");

        BridgeConnection.Status status = connection.status();
        assertEquals(BridgeConnection.State.WAITING, status.state());
        assertTrue(status.lastError().orElseThrow().contains("1001"));
    }

    @Test
    void keepsKnowingWhichChannelsTheBotBridgesWhileTheLineIsDown() {
        connected();

        transport.listener().onClose(1006, "gone");

        assertTrue(connection.bridges("global"));
        assertFalse(connection.bridges("staff"));
        assertFalse(connection.connected());
    }

    @Test
    void stopsTryingWhenTheBotSpeaksAnotherProtocolAndTriesAgainWhenAskedTo() {
        connection.start();
        transport.listener().onOpen();

        transport.listener().onText("{\"type\":\"welcome\",\"protocol\":2,\"channels\":[]}");

        assertEquals(BridgeConnection.State.INCOMPATIBLE, connection.status().state());
        assertTrue(connection.status().lastError().orElseThrow().contains("protocol 2"));
        assertEquals(0, scheduler.pending());

        connection.start();

        assertEquals(2, transport.attempts.size());
    }

    @Test
    void stopsTryingWhenTheBotClosesWithTheProtocolCode() {
        connection.start();
        transport.listener().onOpen();

        transport.listener().onClose(4002, "protocol mismatch");

        assertEquals(BridgeConnection.State.INCOMPATIBLE, connection.status().state());
        assertEquals(0, scheduler.pending());
    }

    @Test
    void ignoresWhatAReplacedLineSays() {
        connected();
        BridgeTransport.Listener old = transport.listener();

        connection.reconnect();
        old.onClose(1006, "late");
        old.onText("{\"type\":\"query\",\"id\":\"a\",\"what\":\"tps\"}");

        assertEquals(BridgeConnection.State.CONNECTING, connection.status().state());
        assertEquals(2, transport.attempts.size());
        assertTrue(frames.isEmpty());
    }

    @Test
    void holdsChatLinesAndEventsWhileDownAndSendsThemInOrderOnceWelcomed() {
        connection.start();
        connection.send(new OutFrame.Chat("global", JAVI, "uno", false));
        connection.send(new OutFrame.Event(EventKind.JOIN, Optional.of(JAVI), Optional.empty(), Optional.empty()));
        assertTrue(transport.sent.isEmpty());

        transport.listener().onOpen();
        transport.listener().onText(WELCOME);

        assertEquals(List.of("hello", "chat", "event"), transport.types());
    }

    @Test
    void dropsWhatWaitedTooLongAndWhatDoesNotFit() {
        connection.start();
        connection.send(new OutFrame.Chat("global", JAVI, "old", false));
        now = now.plusSeconds(61);
        for (int i = 0; i < 101; i++) {
            connection.send(new OutFrame.Chat("global", JAVI, "n" + i, false));
        }

        transport.listener().onOpen();
        transport.listener().onText(WELCOME);

        // 1 old line pushed out by the limit of 100, and none of the 100 kept is old
        assertEquals(101, transport.sent.size());
        assertEquals(2, connection.status().dropped());
    }

    @Test
    void doesNotHoldAReplyForLater() {
        connection.start();

        connection.send(OutFrame.Reply.ok("q", new JsonObject()));

        assertEquals(1, connection.status().dropped());
    }

    @Test
    void sendsRightAwayWhileConnected() {
        connected();

        connection.send(new OutFrame.Chat("global", JAVI, "hola", false));

        assertEquals(List.of("hello", "chat"), transport.types());
        assertEquals(2, connection.status().sent());
    }

    @Test
    void handsTheChatAndQueriesOfTheBotToTheConsumerButOnlyAfterTheWelcome() {
        connection.start();
        transport.listener().onOpen();
        transport.listener().onText("{\"type\":\"query\",\"id\":\"early\",\"what\":\"tps\"}");
        assertTrue(frames.isEmpty());

        transport.listener().onText(WELCOME);
        transport.listener().onText("{\"type\":\"query\",\"id\":\"q\",\"what\":\"status\"}");
        transport.listener().onText("{\"type\":\"chat\",\"channel\":\"global\",\"author\":{\"discordId\":\"123456789012345678\",\"ign\":\"Tez\"},\"text\":\"hi\"}");

        assertEquals(List.of(new InFrame.Query("q", QueryKind.STATUS), new InFrame.Chat("global", "123456789012345678", "Tez", "hi", List.of(), 0)), frames);
    }

    @Test
    void handsOverAChatLineWithItsImagesAndCountsTheImagesThatWereLeftOut() {
        connected();
        String valid = "{\"url\":\"https://cdn.discordapp.com/a.png\",\"name\":\"a.png\",\"type\":\"image/png\",\"size\":10,\"width\":null,\"height\":null,\"spoiler\":false}";
        String invalid = "{\"url\":\"http://cdn.discordapp.com/a.png\",\"name\":\"a.png\",\"type\":\"image/png\",\"size\":10,\"width\":null,\"height\":null,\"spoiler\":false}";

        transport.listener().onText("{\"type\":\"chat\",\"channel\":\"global\",\"author\":{\"discordId\":\"123456789012345678\",\"ign\":\"Tez\"},\"text\":\"\",\"media\":[" + valid + "," + invalid + "]}");

        InFrame.Chat chat = (InFrame.Chat) frames.get(0);
        assertEquals(1, chat.media().size());
        assertEquals(1, connection.status().dropped());
    }

    @Test
    void countsAnInvalidFrameAndKeepsTheLine() {
        connected();

        transport.listener().onText("not json");
        transport.listener().onText("{\"type\":\"telemetry\"}");

        assertEquals(BridgeConnection.State.CONNECTED, connection.status().state());
        assertEquals(1, connection.status().dropped());
        assertTrue(connection.status().lastError().orElseThrow().contains("invalid"));
    }

    @Test
    void remembersWhatTheBotSaysWhenItIsUnhappy() {
        connected();

        transport.listener().onText("{\"type\":\"error\",\"code\":\"rate\",\"message\":\"too many frames\"}");

        assertTrue(connection.status().lastError().orElseThrow().contains("rate"));
    }

    @Test
    void dropsALineThatHasBeenSilentForTooLongButKeepsOneThatIsPinged() {
        connected();
        now = now.plusSeconds(60);
        scheduler.runNext();
        transport.listener().onPing();
        now = now.plusSeconds(60);
        scheduler.runNext();
        assertEquals(BridgeConnection.State.CONNECTED, connection.status().state());

        now = now.plusSeconds(100);
        scheduler.runNext();

        assertEquals(BridgeConnection.State.WAITING, connection.status().state());
        assertTrue(transport.closed > 0);
    }

    @Test
    void stopsForGood() {
        connected();
        connection.send(new OutFrame.Chat("global", JAVI, "x", false));

        connection.stop();

        assertEquals(BridgeConnection.State.OFF, connection.status().state());
        assertEquals(0, scheduler.pending());
        assertTrue(transport.closed > 0);
        assertFalse(connection.bridges("global"));
    }

    @Test
    void waitsForTheLastFrameToGetThroughBeforeClosingTheLine() throws Exception {
        connected();
        CompletableFuture<Void> inFlight = new CompletableFuture<>();
        transport.sendResult = inFlight;
        connection.send(new OutFrame.Event(EventKind.STOPPING, Optional.empty(), Optional.empty(), Optional.empty()));
        Thread late = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            inFlight.complete(null);
        });
        late.start();

        connection.stop();
        late.join();

        assertEquals(List.of("hello", "event"), transport.types());
        assertEquals("stopping", JsonParser.parseString(transport.sent.get(1)).getAsJsonObject().get("kind").getAsString());
        assertFalse(transport.closedWhileSending);
        assertEquals(BridgeConnection.State.OFF, connection.status().state());
    }

    @Test
    void anEventReportedBeforeTheConnectionStartsIsSentAfterTheHello() {
        connection.send(new OutFrame.Event(EventKind.STARTED, Optional.empty(), Optional.empty(), Optional.empty()));
        assertTrue(transport.sent.isEmpty());

        connection.start();
        transport.listener().onOpen();
        transport.listener().onText(WELCOME);

        assertEquals(List.of("hello", "event"), transport.types());
        assertEquals("started", JsonParser.parseString(transport.sent.get(1)).getAsJsonObject().get("kind").getAsString());
    }

    @Test
    void everyAttemptSaysHelloWithTheSameStartTime() {
        connected();
        connection.reconnect();
        transport.listener().onOpen();

        List<String> startTimes = transport.sent.stream()
                .map(text -> JsonParser.parseString(text).getAsJsonObject())
                .filter(frame -> frame.get("type").getAsString().equals("hello"))
                .map(frame -> frame.get("startedAt").getAsString())
                .toList();

        assertEquals(List.of("2026-10-12T10:00:00Z", "2026-10-12T10:00:00Z"), startTimes);
    }

    @Test
    void reconnectsAtOnceAndForgetsTheFailures() {
        transport.connectResult = CompletableFuture.failedFuture(new IOException("down"));
        connection.start();
        scheduler.runNext();
        transport.connectResult = CompletableFuture.completedFuture(null);

        connection.reconnect();

        assertEquals(BridgeConnection.State.CONNECTING, connection.status().state());
        transport.listener().onOpen();
        transport.listener().onText(WELCOME);
        assertTrue(connection.connected());
    }
}
