package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.message.MediaView;
import com.panita.enriquecraft.discord.message.ReplyView;
import com.panita.enriquecraft.discord.protocol.InFrame;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Shows in the game what linked Discord members write. The bot has already checked that the author is linked; the mod only
 * trusts the shape of the frame and shows the text as plain text, under the member's Minecraft name and a tag that tells
 * where it came from.
 */
public final class DiscordDelivery {

    private final ChatService chat;
    private final ChannelRegistry channels;
    private final MessageFormatter formatter;
    private final DiscordConfig config;
    private final MediaView media;
    private final MediaPreviews previews;
    private final ReplyView replies;

    public DiscordDelivery(ChatService chat, ChannelRegistry channels, MessageFormatter formatter, DiscordConfig config, MediaView media,
                           MediaPreviews previews, ReplyView replies) {
        this.chat = chat;
        this.channels = channels;
        this.formatter = formatter;
        this.config = config;
        this.media = media;
        this.previews = previews;
        this.replies = replies;
    }

    /** Called on the server thread. A message for a channel this server does not have, or with nothing to say, is dropped. */
    public void deliver(MinecraftServer server, InFrame.Chat frame) {
        Optional<ChannelDefinition> channel = channels.find(frame.channel());
        String text = PlainText.chat(frame.text());
        List<String> hosts = MediaLinks.hostsOf(config.mediaHosts.get());
        // The bot checks the addresses too, but the administrator decides where a link in the game may lead
        List<MediaEntry> allowed = frame.media().stream().filter(entry -> MediaLinks.allows(entry, hosts)).toList();
        List<Component> images = allowed.stream().map(media::link).toList();
        if (channel.isEmpty() || (text.isEmpty() && images.isEmpty())) {
            return;
        }
        Component sender = sender(frame.ign());
        // What it answers goes first, as a line of its own, so the reply reads as the answer
        if (config.showReplies.get()) {
            frame.reply().ifPresent(reply -> chat.deliverLine(server, channel.get(), replies.line(sender, reply)));
        }
        List<ServerPlayer> readers = chat.deliverExternal(server, channel.get(), sender, text, images);
        // The client companion draws the image itself, but only one from a host it trusts on its own
        previews.send(readers, allowed.stream().filter(MediaLinks::isTrusted).toList());
    }

    private Component sender(String ign) {
        Component hover = formatter.format(Message.plain(DiscordMessages.Delivery.HOVER));
        return Component.empty()
                .append(formatter.format(Message.plain(config.deliveryPrefix.get())))
                .append(Component.literal(" "))
                .append(Component.literal(ign).withStyle(ChatFormatting.WHITE))
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
    }
}
