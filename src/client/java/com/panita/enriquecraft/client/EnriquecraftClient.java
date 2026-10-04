package com.panita.enriquecraft.client;

import com.panita.enriquecraft.client.ui.ServerUiNetworking;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.core.network.HelloC2S;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.EnumSet;
import java.util.Set;

/**
 * The client companion's entrypoint. It only adds enhancements: on a server without the mod it
 * does nothing, and nothing it does changes game rules.
 */
public final class EnriquecraftClient implements ClientModInitializer {

    /** The enhancements this client implements; grows as each one is added. */
    private static final Set<ClientFeature> SUPPORTED = EnumSet.of(ClientFeature.CUSTOM_UI);

    @Override
    public void onInitializeClient() {
        ServerUiNetworking.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> announce());
    }

    private static void announce() {
        // A server without the mod never registered the hello channel, and sending to it would throw.
        if (ClientPlayNetworking.canSend(HelloC2S.TYPE)) {
            ClientPlayNetworking.send(HelloC2S.announcing(SUPPORTED));
        }
    }
}
