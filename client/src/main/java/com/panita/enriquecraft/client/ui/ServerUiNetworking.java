package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.CloseUiS2C;
import com.panita.enriquecraft.core.network.OpenUiS2C;
import com.panita.enriquecraft.core.network.UpdateUiS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.Optional;

/**
 * Receives the server's instructions about its screens. Instructions about a screen that is no
 * longer the one on display are ignored.
 */
public final class ServerUiNetworking {

    private ServerUiNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenUiS2C.TYPE, (payload, context) -> {
            // A screen that replaces another grows from the other's shape instead of popping up.
            ServerUiScreen.Bounds previous = context.client().gui.screen() instanceof ServerUiScreen old ? old.panelBounds() : null;
            context.client().setScreenAndShow(
                    new ServerUiScreen(payload.sessionId(), payload.title(), payload.icon(), payload.root(), previous));
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateUiS2C.TYPE, (payload, context) ->
                showing(context.client(), payload.sessionId()).ifPresent(screen -> screen.update(payload.root())));
        ClientPlayNetworking.registerGlobalReceiver(CloseUiS2C.TYPE, (payload, context) ->
                showing(context.client(), payload.sessionId()).ifPresent(screen -> context.client().setScreenAndShow(null)));
    }

    private static Optional<ServerUiScreen> showing(Minecraft client, int sessionId) {
        return client.gui.screen() instanceof ServerUiScreen screen && screen.sessionId() == sessionId
                ? Optional.of(screen)
                : Optional.empty();
    }
}
