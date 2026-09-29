package com.panita.enriquecraft.message.channel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shows boss bars to players and remembers them by id, so a bar can be updated or hidden later.
 * <p>
 * Boss bars are per player. Only the server thread may call this class.
 */
public final class BossBarChannel {

    private final Map<UUID, Map<String, ServerBossEvent>> barsByPlayer = new HashMap<>();

    /**
     * Shows a boss bar to a player, or updates it if a bar with the same id is already visible.
     */
    public void show(ServerPlayer player, String id, Component title, BossEvent.BossBarColor color,
                     BossEvent.BossBarOverlay overlay, float progress) {
        Map<String, ServerBossEvent> bars = barsByPlayer.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        ServerBossEvent bar = bars.get(id);
        if (bar == null) {
            bar = new ServerBossEvent(UUID.randomUUID(), title, color, overlay);
            bar.addPlayer(player);
            bars.put(id, bar);
        }
        bar.setName(title);
        bar.setColor(color);
        bar.setOverlay(overlay);
        bar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
    }

    public void hide(ServerPlayer player, String id) {
        Map<String, ServerBossEvent> bars = barsByPlayer.get(player.getUUID());
        if (bars == null) {
            return;
        }
        ServerBossEvent bar = bars.remove(id);
        if (bar != null) {
            bar.removeAllPlayers();
        }
        if (bars.isEmpty()) {
            barsByPlayer.remove(player.getUUID());
        }
    }

    /**
     * Discards every bar of a player, for example when the player disconnects.
     */
    public void removeAll(UUID playerId) {
        Map<String, ServerBossEvent> bars = barsByPlayer.remove(playerId);
        if (bars != null) {
            bars.values().forEach(ServerBossEvent::removeAllPlayers);
        }
    }

    /**
     * Discards every bar of every player, for example when the server stops.
     */
    public void clear() {
        barsByPlayer.values().forEach(bars -> bars.values().forEach(ServerBossEvent::removeAllPlayers));
        barsByPlayer.clear();
    }
}
