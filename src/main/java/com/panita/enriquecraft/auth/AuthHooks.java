package com.panita.enriquecraft.auth;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.auth.service.Addresses;
import com.panita.enriquecraft.auth.service.LoginGate;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private AuthHooks(LoginGate gate, RefusalView refusals) {
        this.gate = gate;
        this.refusals = refusals;
    }

    static void install(LoginGate gate, RefusalView refusals) {
        instance = new AuthHooks(gate, refusals);
    }

    public static @Nullable AuthHooks get() {
        return instance;
    }

    /**
     * Decides about a player who just said their name. Calls exactly one of the two actions, possibly on
     * another thread.
     *
     * @param proceed continues the login, proving the player the given way
     * @param refuse  ends the connection with the reason
     */
    public void admit(MinecraftServer server, Connection connection, String name, Consumer<AuthMode> proceed,
                      Consumer<Component> refuse) {
        gate.admit(server, Addresses.of(connection.getRemoteAddress()), name).whenComplete((verdict, error) -> {
            if (error != null) {
                LOGGER.error("Could not decide about {}", name, error);
                refuse.accept(refusals.of(new LoginGate.Verdict.Unavailable()).orElseThrow());
            } else if (verdict instanceof LoginGate.Verdict.Proceed proceedVerdict) {
                proceed.accept(proceedVerdict.mode());
            } else {
                refuse.accept(refusals.of(verdict).orElseThrow());
            }
        });
    }
}
