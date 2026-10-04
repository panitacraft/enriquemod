package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiSelectC2S;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
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
 * behind them, nothing else would stop a player from using the buttons of a screen that is gone,
 * so every press and every value the client reports is checked here before anything runs: the
 * client is untrusted.
 * <p>
 * A session ends when the screen is closed from either side, when another screen replaces it, and
 * when the player dies or disconnects.
 */
public final class UiSessions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiSessions.class);

    /** A person cannot act faster than this; anything quicker is dropped. */
    private static final long MIN_ACTION_INTERVAL_NANOS = 50_000_000L;

    /** What became of an action reported by a client. */
    enum ActionResult {
        HANDLED,
        /** The screen it belongs to is gone or was replaced, which happens in normal play. */
        STALE,
        TOO_FAST,
        /** The element does not exist in the screen's current description, or is not that kind of element. */
        UNKNOWN_ELEMENT,
        /** The action carries something the element does not accept. */
        INVALID
    }

    /** Which screen a player is looking at, and where its next description must start numbering. */
    record Showing(int sessionId, int nextElementId) {
    }

    private static final class Session {
        private final int id;
        private final UiMenu menu;
        private Map<Integer, Consumer<UiClick>> handlers;
        private Map<Integer, UiInputHandler> inputs;
        private Map<Integer, UiSelectHandler> selects;
        private int nextElementId;
        private long lastAction;
        private boolean acted;

        private Session(int id, UiMenu menu, UiLayout layout) {
            this.id = id;
            this.menu = menu;
            replace(layout);
        }

        private void replace(UiLayout layout) {
            handlers = layout.handlers();
            inputs = layout.inputs();
            selects = layout.selects();
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

    /** Runs the action behind a value reported by a client, if the value is valid. */
    public void submit(ServerPlayer player, UiSubmitC2S submit) {
        submit(player.getUUID(), player, submit);
    }

    /** Runs the action behind an option reported by a client, if the option is valid. */
    public void select(ServerPlayer player, UiSelectC2S select) {
        select(player.getUUID(), player, select);
    }

    ActionResult click(UUID playerId, ServerPlayer player, UiClickC2S click) {
        Session session = sessions.get(playerId);
        Optional<ActionResult> rejection = admit(session, click.sessionId());
        if (rejection.isPresent()) {
            return rejection.get();
        }
        if (click.button() < 0 || click.button() > 1) {
            LOGGER.warn("{} sent a press with mouse button {}", playerId, click.button());
            return ActionResult.INVALID;
        }
        Consumer<UiClick> handler = session.handlers.get(click.elementId());
        if (handler == null) {
            // Also what a press looks like when it crossed the screen's update on the wire.
            return ActionResult.UNKNOWN_ELEMENT;
        }
        return run(playerId, () -> handler.accept(new UiClick(player, click.button(), click.shift())));
    }

    ActionResult submit(UUID playerId, ServerPlayer player, UiSubmitC2S submit) {
        Session session = sessions.get(playerId);
        Optional<ActionResult> rejection = admit(session, submit.sessionId());
        if (rejection.isPresent()) {
            return rejection.get();
        }
        UiInputHandler input = session.inputs.get(submit.elementId());
        if (input == null) {
            return ActionResult.UNKNOWN_ELEMENT;
        }
        if (submit.text().length() > input.maxLength() || submit.text().chars().anyMatch(Character::isISOControl)) {
            LOGGER.warn("{} sent a value the field does not accept", playerId);
            return ActionResult.INVALID;
        }
        return run(playerId, () -> input.action().accept(new UiSubmit(player, submit.text())));
    }

    ActionResult select(UUID playerId, ServerPlayer player, UiSelectC2S select) {
        Session session = sessions.get(playerId);
        Optional<ActionResult> rejection = admit(session, select.sessionId());
        if (rejection.isPresent()) {
            return rejection.get();
        }
        UiSelectHandler dropdown = session.selects.get(select.elementId());
        if (dropdown == null) {
            return ActionResult.UNKNOWN_ELEMENT;
        }
        if (select.option() < 0 || select.option() >= dropdown.optionCount()) {
            LOGGER.warn("{} chose an option a dropdown does not have", playerId);
            return ActionResult.INVALID;
        }
        return run(playerId, () -> dropdown.action().accept(new UiSelect(player, select.option())));
    }

    /** Checks that the action belongs to the player's current screen and is not coming too fast. */
    private Optional<ActionResult> admit(Session session, int sessionId) {
        if (session == null || session.id != sessionId) {
            return Optional.of(ActionResult.STALE);
        }
        long now = clock.getAsLong();
        if (session.acted && now - session.lastAction < MIN_ACTION_INTERVAL_NANOS) {
            return Optional.of(ActionResult.TOO_FAST);
        }
        session.acted = true;
        session.lastAction = now;
        return Optional.empty();
    }

    private static ActionResult run(UUID playerId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            LOGGER.error("A screen action failed for {}", playerId, e);
        }
        return ActionResult.HANDLED;
    }
}
