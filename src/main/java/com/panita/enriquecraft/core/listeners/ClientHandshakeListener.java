package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.HelloC2S;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Receives the hello of players who run the client companion and forgets them when they leave.
 * The payload type is registered here for both sides: the client needs it to send the hello.
 */
public final class ClientHandshakeListener implements ModListener {

    private final ClientCapabilities capabilities;

    public ClientHandshakeListener(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void register() {
        PayloadTypeRegistry.serverboundPlay().register(HelloC2S.TYPE, HelloC2S.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HelloC2S.TYPE,
                (hello, context) -> capabilities.accept(context.player().getUUID(), hello));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> capabilities.forget(handler.getPlayer().getUUID()));
    }
}
