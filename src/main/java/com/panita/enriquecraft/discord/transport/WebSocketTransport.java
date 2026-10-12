package com.panita.enriquecraft.discord.transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * The bridge over the JDK's own WebSocket, with no library added. The JDK allows one send at a time, so sends are chained;
 * incoming messages may arrive in pieces and are joined before the listener sees them. The JDK answers the bot's pings by
 * itself.
 */
public final class WebSocketTransport implements BridgeTransport {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client = HttpClient.newHttpClient();
    private WebSocket socket;
    private CompletableFuture<Void> sends = CompletableFuture.completedFuture(null);

    @Override
    public synchronized CompletableFuture<Void> connect(URI uri, String key, Listener listener) {
        close();
        sends = CompletableFuture.completedFuture(null);
        return client.newWebSocketBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .header("Authorization", "Bearer " + key)
                .buildAsync(uri, new Receiver(listener, this::opened))
                .thenAccept(ignored -> { });
    }

    /** The socket is known as soon as it opens, so a listener may send from its own onOpen. */
    private synchronized void opened(WebSocket opened) {
        socket = opened;
    }

    @Override
    public synchronized CompletableFuture<Void> send(String text) {
        WebSocket current = socket;
        if (current == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("not connected"));
        }
        // A failed send must not stop the ones after it from being tried.
        sends = sends.handle((ok, error) -> null).thenCompose(ignored -> current.sendText(text, true)).thenApply(ignored -> null);
        return sends;
    }

    @Override
    public synchronized void close() {
        if (socket != null) {
            socket.abort();
            socket = null;
        }
    }

    /** Joins the pieces of a message and passes what happens to the listener. */
    private static final class Receiver implements WebSocket.Listener {

        private final Listener listener;
        private final Consumer<WebSocket> opened;
        private final StringBuilder partial = new StringBuilder();

        Receiver(Listener listener, Consumer<WebSocket> opened) {
            this.listener = listener;
            this.opened = opened;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            opened.accept(webSocket);
            listener.onOpen();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String text = partial.toString();
                partial.setLength(0);
                listener.onText(text);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            listener.onPing();
            webSocket.request(1);
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            listener.onClose(statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listener.onError(error);
        }
    }
}
