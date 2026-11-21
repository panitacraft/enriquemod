package com.panita.enriquecraft.core.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiClosedC2S;
import com.panita.enriquecraft.core.network.UiDropC2S;
import com.panita.enriquecraft.core.network.UiSelectC2S;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
import com.panita.enriquecraft.core.ui.UiSessions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Connects the client companion's screens to the server: hands what clients report, presses and typed values, to
 * {@link UiSessions}, and ends a player's screen when they die or leave, as a vanilla container would close.
 */
public final class UiNetworkListener implements ModListener {

    private final UiSessions sessions;

    public UiNetworkListener(UiSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public void register() {

        ServerPlayNetworking.registerGlobalReceiver(UiClickC2S.TYPE,
                (click, context) -> sessions.click(context.player(), click));
        ServerPlayNetworking.registerGlobalReceiver(UiSubmitC2S.TYPE,
                (submit, context) -> sessions.submit(context.player(), submit));
        ServerPlayNetworking.registerGlobalReceiver(UiSelectC2S.TYPE,
                (select, context) -> sessions.select(context.player(), select));
        ServerPlayNetworking.registerGlobalReceiver(UiDropC2S.TYPE,
                (drop, context) -> sessions.drop(context.player(), drop));
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
