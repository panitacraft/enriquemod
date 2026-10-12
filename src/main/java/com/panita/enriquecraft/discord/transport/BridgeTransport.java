package com.panita.enriquecraft.discord.transport;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * The line to the bot, seen as text going back and forth. The WebSocket is the first implementation; a relay through the
 * API could replace it without the connection logic changing. Callbacks come from whichever thread the transport uses.
 */
public interface BridgeTransport {

    /** What happens on a line. */
    interface Listener {

        void onOpen();

        void onText(String text);

        /** The bot pinged, which is how a connection that is alive but quiet is told from a dead one. */
        void onPing();

        void onClose(int code, String reason);

        void onError(Throwable error);
    }

    /**
     * Opens a line, replacing any that is open.
     *
     * @param key the secret the bot expects
     * @return a future that fails if the line could not be opened
     */
    CompletableFuture<Void> connect(URI uri, String key, Listener listener);

    /** Sends a text. Texts are sent in the order they were given. */
    CompletableFuture<Void> send(String text);

    /** Closes the line, if there is one. */
    void close();
}
