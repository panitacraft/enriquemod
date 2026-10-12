package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.protocol.InFrame;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;

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

    public DiscordDelivery(ChatService chat, ChannelRegistry channels, MessageFormatter formatter, DiscordConfig config) {
        this.chat = chat;
        this.channels = channels;
        this.formatter = formatter;
        this.config = config;
    }

    /** Called on the server thread. A message for a channel this server does not have, or with nothing to say, is dropped. */
    public void deliver(MinecraftServer server, InFrame.Chat frame) {
        Optional<ChannelDefinition> channel = channels.find(frame.channel());
        String text = PlainText.chat(frame.text());
        if (channel.isEmpty() || text.isEmpty()) {
            return;
        }
        chat.deliverExternal(server, channel.get(), sender(frame.ign()), text);
    }

    private Component sender(String ign) {
        Component hover = formatter.format(Message.plain(DiscordMessages.Delivery.HOVER));
        return Component.empty()
                .append(Component.literal("[" + config.deliveryTag.get() + "] ").withStyle(ChatFormatting.BLUE))
                .append(Component.literal(ign).withStyle(ChatFormatting.WHITE))
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
    }
}
