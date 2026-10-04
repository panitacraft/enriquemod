package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.network.CloseUiS2C;
import com.panita.enriquecraft.core.network.OpenUiS2C;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiClosedC2S;
import com.panita.enriquecraft.core.network.UpdateUiS2C;
import com.panita.enriquecraft.core.ui.UiSessions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Connects the client companion's screens to the server: registers their payloads (for both sides,
 * since the client needs the types too), hands what clients report to {@link UiSessions}, and ends a
 * player's screen when they die or leave, as a vanilla container would close.
 */
public final class UiNetworkListener implements ModListener {

    private final UiSessions sessions;

    public UiNetworkListener(UiSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public void register() {
        PayloadTypeRegistry.clientboundPlay().register(OpenUiS2C.TYPE, OpenUiS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdateUiS2C.TYPE, UpdateUiS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CloseUiS2C.TYPE, CloseUiS2C.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiClickC2S.TYPE, UiClickC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UiClosedC2S.TYPE, UiClosedC2S.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UiClickC2S.TYPE,
                (click, context) -> sessions.click(context.player(), click));
        ServerPlayNetworking.registerGlobalReceiver(UiClosedC2S.TYPE,
                (closed, context) -> sessions.closedByClient(context.player().getUUID(), closed.sessionId()));

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> sessions.end(handler.getPlayer().getUUID()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                sessions.end(player.getUUID());
            }
        });
    }
}
