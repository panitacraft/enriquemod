package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.network.UiClickC2S;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * The screens currently shown by the client companion, one per player. With no vanilla container
 * behind them, nothing else would stop a player from pressing the buttons of a screen that is gone,
 * so every press the client reports is checked here before anything runs: the client is untrusted.
 * <p>
 * A session ends when the screen is closed from either side, when another screen replaces it, and
 * when the player dies or disconnects.
 */
public final class UiSessions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiSessions.class);

    /** A person cannot press buttons faster than this; anything quicker is dropped. */
    private static final long MIN_CLICK_INTERVAL_NANOS = 50_000_000L;

    /** What became of a press reported by a client. */
    enum ClickResult {
        HANDLED,
        /** The screen it belongs to is gone or was replaced, which happens in normal play. */
        STALE,
        TOO_FAST,
        /** The button does not exist in the screen's current description. */
        UNKNOWN_BUTTON,
        /** The mouse button is not one the screen accepts. */
        INVALID
    }

    /** Which screen a player is looking at, and where its next description must start numbering. */
    record Showing(int sessionId, int nextElementId) {
    }

    private static final class Session {
        private final int id;
        private final UiMenu menu;
        private Map<Integer, Consumer<UiClick>> handlers;
        private int nextElementId;
        private long lastClick;
        private boolean clicked;

        private Session(int id, UiMenu menu, UiLayout layout) {
            this.id = id;
            this.menu = menu;
            replace(layout);
        }

        private void replace(UiLayout layout) {
            handlers = layout.handlers();
            nextElementId = layout.nextId();
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private int nextSessionId;

    public UiSessions() {
        this(System::nanoTime);
    }

    UiSessions(LongSupplier clock) {
        this.clock = clock;
    }

    /** Starts showing a menu to a player, ending whatever they were looking at. */
    int begin(UUID player, UiMenu menu, UiLayout layout) {
        Session session = new Session(++nextSessionId, menu, layout);
        Session replaced = sessions.put(player, session);
        // Opening the same menu again is not closing it.
        if (replaced != null && replaced.menu != menu) {
            replaced.menu.closed();
        }
        return session.id;
    }

    Optional<Showing> showing(UUID player, UiMenu menu) {
        Session session = sessions.get(player);
        if (session == null || session.menu != menu) {
            return Optional.empty();
        }
        return Optional.of(new Showing(session.id, session.nextElementId));
    }

    /** Swaps in the new description of the screen the player is looking at. */
    void update(UUID player, UiLayout layout) {
        Session session = sessions.get(player);
        if (session != null) {
            session.replace(layout);
        }
    }

    /** Ends the player's session, if any, and returns its id. */
    public OptionalInt end(UUID player) {
        Session session = sessions.remove(player);
        if (session == null) {
            return OptionalInt.empty();
        }
        session.menu.closed();
        return OptionalInt.of(session.id);
    }

    /** The client reports that a screen is gone; reports about screens that already ended are ignored. */
    public void closedByClient(UUID player, int sessionId) {
        Session session = sessions.get(player);
        if (session != null && session.id == sessionId && sessions.remove(player, session)) {
            session.menu.closed();
        }
    }

    /** Runs the action behind a press reported by a client, if the press is valid. */
    public void click(ServerPlayer player, UiClickC2S click) {
        click(player.getUUID(), player, click);
    }

    ClickResult click(UUID playerId, ServerPlayer player, UiClickC2S click) {
        Session session = sessions.get(playerId);
        if (session == null || session.id != click.sessionId()) {
            return ClickResult.STALE;
        }
        long now = clock.getAsLong();
        if (session.clicked && now - session.lastClick < MIN_CLICK_INTERVAL_NANOS) {
            return ClickResult.TOO_FAST;
        }
        session.clicked = true;
        session.lastClick = now;

        if (click.button() < 0 || click.button() > 1) {
            LOGGER.warn("{} sent a press with mouse button {}", playerId, click.button());
            return ClickResult.INVALID;
        }
        Consumer<UiClick> handler = session.handlers.get(click.elementId());
        if (handler == null) {
            // Also what a press looks like when it crossed the screen's update on the wire.
            return ClickResult.UNKNOWN_BUTTON;
        }
        try {
            handler.accept(new UiClick(player, click.button(), click.shift()));
        } catch (RuntimeException e) {
            LOGGER.error("A screen action failed for {}", playerId, e);
        }
        return ClickResult.HANDLED;
    }
}
