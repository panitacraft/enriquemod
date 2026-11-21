package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.HelloC2S;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Receives the hello of players who run the client companion and forgets them when they leave.
 * The payload types themselves are registered by {@code NetworkProtocol.registerPayloads}.
 */
public final class ClientHandshakeListener implements ModListener {

    private final ClientCapabilities capabilities;

    public ClientHandshakeListener(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(HelloC2S.TYPE,
                (hello, context) -> capabilities.accept(context.player().getUUID(), hello));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> capabilities.forget(handler.getPlayer().getUUID()));
    }
}
