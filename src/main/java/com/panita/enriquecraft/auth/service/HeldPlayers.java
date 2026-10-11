package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.data.HeldState;
import com.panita.enriquecraft.auth.message.LoginView;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.ClientFeature;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The players who are in the game but have not entered their PIN yet. A held player is moved to the
 * waiting spot, shown the PIN screen, cannot do anything but use it (every other thing their client
 * sends is dropped, and nothing can hurt them). They leave this state
 * by entering the right PIN, by being kicked, or by disconnecting; in every case they get back to where
 * they really were.
 */
public final class HeldPlayers {

    /** How long to wait for the client companion to announce itself before showing a chest instead. */
    private static final int ANNOUNCEMENT_WAIT_TICKS = 20;
    private static final int REOPEN_DELAY_TICKS = 3;
    private static final int TICKS_PER_SECOND = 20;
    /** The explanation stays on screen at least this long, even when the chest opens at once. */
    private static final int MIN_INTRO_SECONDS = 2;

    /** The only things a held player's client may say; everything else is dropped before it is handled. */
    private static final Set<Class<?>> ALLOWED_PACKETS = Set.of(
            ServerboundCustomPayloadPacket.class, ServerboundKeepAlivePacket.class, ServerboundPongPacket.class,
            ServerboundClientInformationPacket.class, ServerboundClientTickEndPacket.class,
            ServerboundChunkBatchReceivedPacket.class, ServerboundPlayerLoadedPacket.class,
            ServerboundAcceptTeleportationPacket.class, ServerboundContainerClosePacket.class);

    private static final class Held {
        private final ServerPlayer player;
        private final String address;
        private final PinFlow flow;
        private PinScreens.Screen screen;
        private int ticks;
        private int reopenAt = -1;
        private int introUntil = -1;
        private boolean begun;
        private boolean kicked;
        private volatile boolean dismissed;

        private Held(ServerPlayer player, String address, PinFlow flow) {
            this.player = player;
            this.address = address;
            this.flow = flow;
        }
    }

    private final Map<UUID, Held> held = new ConcurrentHashMap<>();
    private final AccountService accounts;
    private final AttemptLimiter limiter;
    private final AuthConfig config;
    private final RefusalView refusals;
    private final PinScreens screens;
    private final ClientCapabilities capabilities;
    private final LoginView loginView;

    public HeldPlayers(AccountService accounts, AttemptLimiter limiter, AuthConfig config, RefusalView refusals,
                       PinScreens screens, ClientCapabilities capabilities, LoginView loginView) {
        this.accounts = accounts;
        this.limiter = limiter;
        this.config = config;
        this.refusals = refusals;
        this.screens = screens;
        this.capabilities = capabilities;
        this.loginView = loginView;
    }

    public boolean isHeld(ServerPlayer player) {
        return held.containsKey(player.getUUID());
    }

    /**
     * Decides, as a player is placed in the world, whether they must enter a PIN: players who prove
     * themselves with a Microsoft account do not, and neither do those whose last PIN is still remembered
     * for this address.
     */
    public void joining(ServerPlayer player, SocketAddress address) {
        UUID id = player.getUUID();
        String name = player.getGameProfile().name();
        // A singleplayer world is not a server to log in to, whatever account its player has.
        if (!player.level().getServer().isDedicatedServer() || AuthMode.of(id, name) != AuthMode.CRACKED) {
            return;
        }
        String ip = Addresses.of(address);
        if (accounts.hasValidSession(id, ip)) {
            return;
        }
        Optional<Integer> pinLength = accounts.pinLength(id);
        PinEntry entry = new PinEntry(pinLength.orElse(config.pinLength.get()), config.maxAttempts.get(), pinLength.isPresent());
        PinFlow flow = new PinFlow(id, name, entry, accounts, new PinFlow.Listener() {
            @Override
            public void accepted() {
                accept(id, true);
            }

            @Override
            public void wrongPin() {
                limiter.recordFailure(ip);
            }

            @Override
            public void lockedOut() {
                Held holder = held.get(id);
                if (holder != null) {
                    kick(holder, refusals.tooManyAttempts());
                }
            }
        });
        held.put(id, new Held(player, ip, flow));
    }

    /** Called once the player is in the world: sends a held player to the waiting spot. */
    public void begin(ServerPlayer player) {
        Held holder = held.get(player.getUUID());
        if (holder == null) {
            welcomeWithoutPin(player);
            return;
        }
        // A leftover position means the server stopped while they waited: that one is the real one.
        if (accounts.heldAt(player.getUUID()).isEmpty()) {
            accounts.holdAt(player.getUUID(), player.getGameProfile().name(), HeldState.of(player));
        }
        Limbo.enter(player, config.spectatorWhileWaiting.get());
        holder.begun = true;
    }

    /**
     * A player who is not held came in without a PIN: a premium one with their Microsoft account, one with a PIN on
     * a remembered session. Either is told so. One that a crash left in the waiting spot goes back to where they
     * really were.
     */
    private void welcomeWithoutPin(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!player.level().getServer().isDedicatedServer()) {
            return;
        }
        if (AuthMode.of(id, player.getGameProfile().name()) == AuthMode.PREMIUM) {
            loginView.autoLogin(player);
            return;
        }
        if (accounts.heldAt(id).isPresent()) {
            restore(player);
        }
        String ip = Addresses.of(player.connection.getRemoteAddress());
        accounts.sessionRemaining(id, ip).ifPresent(remaining -> loginView.sessionActive(player, remaining));
    }

    public void tick() {
        for (Held holder : held.values()) {
            if (holder.kicked) {
                continue;
            }
            holder.ticks++;
            if (holder.flow.tick() && holder.screen != null) {
                holder.screen.redraw();
            }
            if (!held.containsValue(holder) || holder.kicked) {
                continue;
            }
            if (holder.ticks > config.loginTimeoutSeconds.get() * TICKS_PER_SECOND) {
                kick(holder, refusals.timeout());
            } else if (holder.begun) {
                keepScreenOpen(holder);
            }
        }
    }

    private void keepScreenOpen(Held holder) {
        if (holder.screen == null) {
            if (holder.player.connection.hasClientLoaded()) {
                openOrIntroduce(holder);
            }
        } else if (holder.dismissed) {
            // Closing the screen does not get the player out of waiting: it comes back.
            if (holder.reopenAt < 0) {
                holder.reopenAt = holder.ticks + REOPEN_DELAY_TICKS;
            } else if (holder.ticks >= holder.reopenAt) {
                holder.reopenAt = -1;
                open(holder);
            }
        }
    }

    /**
     * Opens the screen at once for a client that announced the companion. Any other client is first told what it is
     * about to be asked, then gets the chest after the time the administrator chose.
     */
    private void openOrIntroduce(Held holder) {
        if (capabilities.supports(holder.player.getUUID(), ClientFeature.CUSTOM_UI)) {
            open(holder);
            return;
        }
        if (holder.ticks < ANNOUNCEMENT_WAIT_TICKS) {
            return;
        }
        if (holder.introUntil < 0) {
            int seconds = config.introSeconds.get();
            loginView.intro(holder.player, holder.flow, Duration.ofSeconds(Math.max(seconds, MIN_INTRO_SECONDS)));
            holder.introUntil = holder.ticks + seconds * TICKS_PER_SECOND;
        }
        if (holder.ticks >= holder.introUntil) {
            open(holder);
        }
    }

    private void open(Held holder) {
        holder.dismissed = false;
        holder.screen = screens.open(holder.player, holder.flow, () -> holder.dismissed = true);
    }

    /**
     * Lets a held player in.
     *
     * @param remember whether the login is remembered for the session time; staff letting someone in without
     *                 a PIN do not grant that
     */
    private void accept(UUID id, boolean remember) {
        Held holder = held.get(id);
        if (holder == null) {
            return;
        }
        if (remember) {
            accounts.startSession(id, holder.address, Duration.ofHours(config.sessionHours.get()));
            limiter.clear(holder.address);
        }
        held.remove(id);
        if (holder.screen != null) {
            holder.screen.close();
        }
        restore(holder.player);
        if (remember) {
            loginView.sessionActive(holder.player, Duration.ofHours(config.sessionHours.get()));
        } else {
            loginView.letIn(holder.player);
        }
    }

    private void kick(Held holder, Component reason) {
        holder.kicked = true;
        holder.player.connection.disconnect(reason);
    }

    /** Puts the player back where they really were and forgets that they were moved. */
    private void restore(ServerPlayer player) {
        accounts.heldAt(player.getUUID()).ifPresent(position -> Limbo.leave(player, position));
        accounts.clearHeldAt(player.getUUID());
    }

    /**
     * Called as a player leaves, before their data is saved, so a player who was still waiting is saved as they
     * really were.
     */
    public void leaving(ServerPlayer player) {
        if (held.remove(player.getUUID()) != null) {
            restore(player);
        }
    }

    /** Whether a packet from a held player must be dropped: they may only use the screen they are shown. */
    public boolean blocks(ServerPlayer player, Packet<?> packet) {
        if (!held.containsKey(player.getUUID())) {
            return false;
        }
        if (packet instanceof ServerboundContainerClickPacket click) {
            // Only the PIN screen may be clicked, never the player's own inventory behind it.
            return player.containerMenu == player.inventoryMenu || click.containerId() != player.containerMenu.containerId;
        }
        return !ALLOWED_PACKETS.contains(packet.getClass());
    }

    /** Lets a held player in without a PIN, as staff decide, for this time only. Returns whether the player was held. */
    public boolean forceLogin(ServerPlayer player) {
        if (!held.containsKey(player.getUUID())) {
            return false;
        }
        accept(player.getUUID(), false);
        return true;
    }
}
