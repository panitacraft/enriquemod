package com.panita.enriquecraft.auth;

import com.panita.enriquecraft.auth.commands.WhitelistHandler;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.auth.service.Addresses;
import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.auth.service.LoginGate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The way the auth module's mixins reach its services. A mixin is instantiated by Mixin, not by the
 * service registry, so it cannot receive services through a constructor; this is the one static door,
 * installed once when the module loads. Without it (for example if the module failed to start) every mixin
 * leaves vanilla behavior untouched.
 */
public final class AuthHooks {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthHooks.class);
    private static volatile @Nullable AuthHooks instance;

    private final LoginGate gate;
    private final RefusalView refusals;
    private final HeldPlayers held;
    private final WhitelistHandler whitelist;

    private AuthHooks(LoginGate gate, RefusalView refusals, HeldPlayers held, WhitelistHandler whitelist) {
        this.gate = gate;
        this.refusals = refusals;
        this.held = held;
        this.whitelist = whitelist;
    }

    static void install(LoginGate gate, RefusalView refusals, HeldPlayers held, WhitelistHandler whitelist) {
        instance = new AuthHooks(gate, refusals, held, whitelist);
    }

    public static @Nullable AuthHooks get() {
        return instance;
    }

    /**
     * Decides about a player who just said their name. Calls exactly one of the two actions, possibly on
     * another thread.
     *
     * @param proceed continues the login, proving the player the given way, under the given spelling of their name
     * @param refuse  ends the connection with the reason
     */
    public void admit(MinecraftServer server, Connection connection, String name, BiConsumer<AuthMode, String> proceed,
                      Consumer<Component> refuse) {
        gate.admit(server, Addresses.of(connection.getRemoteAddress()), name).whenComplete((verdict, error) -> {
            if (error != null) {
                LOGGER.error("Could not decide about {}", name, error);
                refuse.accept(refusals.of(new LoginGate.Verdict.Unavailable()).orElseThrow());
            } else if (verdict instanceof LoginGate.Verdict.Proceed proceedVerdict) {
                proceed.accept(proceedVerdict.mode(), proceedVerdict.name());
            } else {
                refuse.accept(refusals.of(verdict).orElseThrow());
            }
        });
    }

    /** A player is being placed in the world: decides whether they must enter a PIN. */
    public void playerJoining(Connection connection, ServerPlayer player) {
        held.joining(player, connection.getRemoteAddress());
    }

    /** A player is leaving: if they were waiting for the PIN screen they are put back as they really were. */
    public void leaving(ServerPlayer player) {
        held.leaving(player);
    }

    /** Whether the player has not entered their PIN yet. */
    public boolean isHeld(ServerPlayer player) {
        return held.isHeld(player);
    }

    /** Whether a packet of this player must be dropped. */
    public boolean blocks(ServerPlayer player, Packet<?> packet) {
        return held.blocks(player, packet);
    }

    /** {@code /whitelist add}: adds players, detecting how each logs in unless the mode is given. */
    public int whitelistAdd(CommandSourceStack source, Collection<String> names, Optional<AuthMode> mode) {
        return whitelist.add(source, names, mode);
    }

    /** {@code /whitelist remove}: removes players by name. */
    public int whitelistRemove(CommandSourceStack source, Collection<String> names) {
        return whitelist.remove(source, names);
    }
}
