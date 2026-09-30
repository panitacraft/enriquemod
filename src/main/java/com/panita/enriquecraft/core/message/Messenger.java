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
 * <p>
 * Methods that take a raw string accept text tags, legacy color codes and, for the
 * {@code placeholder} variants, server placeholders. Methods that take a {@link Message} also
 * support named arguments and levels. Never build a raw string from untrusted input.
 * <p>
 * The method set mirrors the owner's Tezzlar III Messenger on purpose: methods that nothing calls
 * yet are intentional. Every method must be called from the server thread.
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

    // Chat: command senders (players, console, command blocks)

    public void send(CommandSourceStack source, String raw) {
        send(source, Message.plain(raw));
    }

    public void prefixedSend(CommandSourceStack source, String raw) {
        send(source, Message.plain(raw).prefixed());
    }

    public void send(CommandSourceStack source, Message message) {
        source.sendSystemMessage(formatter.format(message));
    }

    /**
     * Sends a message only when the sender is not a player, that is, to the console.
     */
    public void consoleSend(CommandSourceStack source, String raw) {
        if (source.getPlayer() == null) {
            send(source, raw);
        }
    }

    // Chat: players

    public void send(ServerPlayer player, String raw) {
        send(player, Message.plain(raw));
    }

    public void prefixedSend(ServerPlayer player, String raw) {
        send(player, Message.plain(raw).prefixed());
    }

    public void send(ServerPlayer player, Message message) {
        player.sendSystemMessage(formatter.format(message));
    }

    /**
     * Sends a message to a player, resolving placeholders for another player, for example
     * {@code %player:name%} becomes the name of the context player.
     */
    public void placeholderSend(ServerPlayer receiver, ServerPlayer context, String raw) {
        receiver.sendSystemMessage(formatter.format(Message.plain(raw), ServerPlaceholderContext.of(context)));
    }

    public void placeholderSend(ServerPlayer receiver, String raw) {
        placeholderSend(receiver, receiver, raw);
    }

    public void prefixedPlaceholderSend(ServerPlayer receiver, ServerPlayer context, String raw) {
        receiver.sendSystemMessage(formatter.format(Message.plain(raw).prefixed(), ServerPlaceholderContext.of(context)));
    }

    public void prefixedPlaceholderSend(ServerPlayer receiver, String raw) {
        prefixedPlaceholderSend(receiver, receiver, raw);
    }

    // Chat: broadcast to every online player and the console

    public void broadcast(MinecraftServer server, String raw) {
        broadcast(server, formatter.format(Message.plain(raw)));
    }

    public void prefixedBroadcast(MinecraftServer server, String raw) {
        broadcast(server, formatter.format(Message.plain(raw).prefixed()));
    }

    /**
     * Broadcasts a message, resolving placeholders for the context player.
     */
    public void placeholderBroadcast(ServerPlayer context, String raw) {
        broadcast(context.level().getServer(), formatter.format(Message.plain(raw), ServerPlaceholderContext.of(context)));
    }

    public void prefixedPlaceholderBroadcast(ServerPlayer context, String raw) {
        broadcast(context.level().getServer(),
                formatter.format(Message.plain(raw).prefixed(), ServerPlaceholderContext.of(context)));
    }

    private void broadcast(MinecraftServer server, Component component) {
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    // Action bar: placeholders resolve for the receiving player

    public void sendActionBar(ServerPlayer player, String raw) {
        sendActionBar(player, Message.plain(raw));
    }

    public void sendActionBar(ServerPlayer player, Message message) {
        player.sendSystemMessage(formatForPlayer(player, message), true);
    }

    public void broadcastActionBar(MinecraftServer server, String raw) {
        server.getPlayerList().getPlayers().forEach(player -> sendActionBar(player, raw));
    }

    // Title: placeholders resolve for the receiving player

    public void showTitle(ServerPlayer player, String rawTitle, String rawSubtitle, TitleTimes times) {
        showTitle(player, Message.plain(rawTitle), Message.plain(rawSubtitle), times);
    }

    public void showTitle(ServerPlayer player, String rawTitle, TitleTimes times) {
        showTitle(player, rawTitle, "", times);
    }

    public void showTitle(ServerPlayer player, Message title, Message subtitle, TitleTimes times) {
        titles.show(player, formatForPlayer(player, title), formatForPlayer(player, subtitle), times);
    }

    public void clearTitle(ServerPlayer player) {
        titles.clear(player);
    }

    // Boss bar: shows a new bar or updates the one with the same id; placeholders resolve for the receiving player

    public void showBossBar(ServerPlayer player, String id, String raw, BossEvent.BossBarColor color,
                            BossEvent.BossBarOverlay overlay, float progress) {
        showBossBar(player, id, Message.plain(raw), color, overlay, progress);
    }

    public void showBossBar(ServerPlayer player, String id, Message title, BossEvent.BossBarColor color,
                            BossEvent.BossBarOverlay overlay, float progress) {
        bossBars.show(player, id, formatForPlayer(player, title), color, overlay, progress);
    }

    public void hideBossBar(ServerPlayer player, String id) {
        bossBars.hide(player, id);
    }

    /**
     * Hides every boss bar of every player, for example on reload or shutdown.
     */
    public void hideAllBossBars() {
        bossBars.clear();
    }

    private Component formatForPlayer(ServerPlayer player, Message message) {
        return formatter.format(message, ServerPlaceholderContext.of(player));
    }
}
