package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.message.channel.BossBarChannel;
import com.panita.enriquecraft.core.message.channel.TitleChannel;
import com.panita.enriquecraft.core.message.channel.TitleTimes;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * Single entry point for every message shown to players, on every channel: chat, action bar,
 * title and boss bar. Only vanilla packets are used, so vanilla clients see everything.
 */
public final class Messenger {

    private final MessageFormatter formatter;
    private final TitleChannel titles;
    private final BossBarChannel bossBars;

    public Messenger(MessageFormatter formatter, TitleChannel titles, BossBarChannel bossBars) {
        this.formatter = formatter;
        this.titles = titles;
        this.bossBars = bossBars;
    }

    // Chat

    public void send(CommandSourceStack source, Message message) {
        source.sendSystemMessage(formatter.format(message, ServerPlaceholderContext.of(source)));
    }

    public void send(ServerPlayer player, Message message) {
        player.sendSystemMessage(format(player, message));
    }

    /**
     * Sends a message to every online player and to the console. Placeholders resolve per recipient.
     */
    public void broadcast(MinecraftServer server, Message message) {
        Component consoleText = formatter.format(message, ServerPlaceholderContext.of(server));
        server.getPlayerList().broadcastSystemMessage(consoleText, player -> format(player, message), false);
    }

    // Action bar

    public void actionBar(ServerPlayer player, Message message) {
        player.sendSystemMessage(format(player, message), true);
    }

    // Title

    public void title(ServerPlayer player, Message title, Message subtitle, TitleTimes times) {
        titles.show(player, format(player, title), format(player, subtitle), times);
    }

    public void clearTitle(ServerPlayer player) {
        titles.clear(player);
    }

    // Boss bar

    public void bossBar(ServerPlayer player, String id, Message title, BossEvent.BossBarColor color,
                        BossEvent.BossBarOverlay overlay, float progress) {
        bossBars.show(player, id, format(player, title), color, overlay, progress);
    }

    public void hideBossBar(ServerPlayer player, String id) {
        bossBars.hide(player, id);
    }

    private Component format(ServerPlayer player, Message message) {
        return formatter.format(message, ServerPlaceholderContext.of(player));
    }
}
