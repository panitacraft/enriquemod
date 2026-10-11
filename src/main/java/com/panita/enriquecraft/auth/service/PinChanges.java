package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The players who are changing their PIN: they enter the current one and then choose a new one, in the same
 * screen used to log in. Unlike a held player they are in the game, so nothing is taken from them; closing
 * the screen just gives the change up. Changing the PIN never kicks anyone: after too many wrong PINs the
 * change is refused and the PIN stays as it was. Wrong current PINs count against the address like those of
 * a login, and an address that is blocked cannot start a change, so an open game is no way to guess the PIN.
 */
public final class PinChanges {

    /** What asking to change a PIN came to. */
    public enum Begin {
        OPENED, NO_PIN, BLOCKED
    }

    private static final class Change {
        private final ServerPlayer player;
        private final String address;
        private final PinFlow flow;
        private PinScreens.Screen screen;

        private Change(ServerPlayer player, String address, PinFlow flow) {
            this.player = player;
            this.address = address;
            this.flow = flow;
        }
    }

    private final Map<UUID, Change> active = new ConcurrentHashMap<>();
    private final AccountService accounts;
    private final AttemptLimiter limiter;
    private final AuthConfig config;
    private final PinScreens screens;
    private final Messenger messenger;

    public PinChanges(AccountService accounts, AttemptLimiter limiter, AuthConfig config, PinScreens screens,
                      Messenger messenger) {
        this.accounts = accounts;
        this.limiter = limiter;
        this.config = config;
        this.screens = screens;
        this.messenger = messenger;
    }

    /** Opens the screen for a player to change their PIN, unless they have none or their address is blocked. */
    public Begin begin(ServerPlayer player) {
        UUID id = player.getUUID();
        Optional<Integer> oldLength = accounts.pinLength(id);
        if (oldLength.isEmpty()) {
            return Begin.NO_PIN;
        }
        String address = Addresses.of(player.connection.getRemoteAddress());
        if (limiter.blockedFor(address).isPresent()) {
            return Begin.BLOCKED;
        }
        cancel(id);
        PinEntry entry = PinEntry.changing(oldLength.get(), config.pinLength.get(), config.maxAttempts.get());
        PinFlow flow = new PinFlow(id, player.getGameProfile().name(), entry, accounts, new PinFlow.Listener() {
            @Override
            public void accepted() {
                finish(id, true);
            }

            @Override
            public void wrongPin() {
                limiter.recordFailure(address);
            }

            @Override
            public void lockedOut() {
                finish(id, false);
            }
        });
        Change change = new Change(player, address, flow);
        active.put(id, change);
        change.screen = screens.open(player, flow, () -> active.remove(id, change));
        return Begin.OPENED;
    }

    public void tick() {
        for (Change change : active.values()) {
            if (change.flow.tick() && change.screen != null) {
                change.screen.redraw();
            }
        }
    }

    /** Gives a change up, for example because the player left. */
    public void cancel(UUID id) {
        Change change = active.remove(id);
        if (change != null && change.screen != null) {
            change.screen.close();
        }
    }

    private void finish(UUID id, boolean changed) {
        Change change = active.remove(id);
        if (change == null) {
            return;
        }
        if (changed) {
            // The new PIN ends the old session, so the player is given a fresh one from this moment.
            accounts.startSession(id, change.address, Duration.ofHours(config.sessionHours.get()));
            limiter.clear(change.address);
        } else {
            messenger.send(change.player, Message.error(AuthMessages.Commands.CHANGE_REFUSED).prefixed());
        }
        change.screen.close();
    }
}
