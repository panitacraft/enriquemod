package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.service.AfkService;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Feeds {@link AfkService} with what shows a player is there: the view turning (looked at twice a second), speaking, and using or
 * attacking blocks, items and entities. Walking alone does not count, since water and minecarts carry a player who is away.
 */
public final class AfkListener implements ModListener {

    private static final int LOOK_INTERVAL_TICKS = 10;
    private static final int REPORT_INTERVAL_TICKS = 20;

    private final AfkService afk;
    private final HeldPlayers held;
    private int ticks;

    public AfkListener(AfkService afk, HeldPlayers held) {
        this.afk = afk;
        this.held = held;
    }

    @Override
    public void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> afk.touch(handler.getPlayer().getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> afk.forget(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % LOOK_INTERVAL_TICKS == 0) {
                server.getPlayerList().getPlayers().forEach(player -> afk.observe(player.getUUID(), player.getYRot(), player.getXRot()));
            }
            if (ticks % REPORT_INTERVAL_TICKS == 0) {
                // The color needs no timer, since it is worked out per line; this is only for who follows the change
                server.getPlayerList().getPlayers().stream().filter(player -> !held.isHeld(player)).forEach(player ->
                        afk.transition(player.getUUID(), afk.isAfk(player)).ifPresent(now -> ChatEvents.AFK.invoker().onAfkChanged(player, now)));
            }
        });
        // Handlers that only watch return true, so they never decide whether the message is sent
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            afk.touch(sender.getUUID());
            return true;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> seen(player));
        UseItemCallback.EVENT.register((player, level, hand) -> seen(player));
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> seen(player));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> seen(player));
    }

    private InteractionResult seen(Player player) {
        afk.touch(player.getUUID());
        return InteractionResult.PASS;
    }
}
