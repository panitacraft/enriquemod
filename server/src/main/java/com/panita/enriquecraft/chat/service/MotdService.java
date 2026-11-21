package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * The text the server list shows under the name of the server. The game rebuilds its status every few seconds and asks for
 * it each time, so what the text says about the server, such as how many players are online, is always current.
 */
public final class MotdService {

    private final MessageFormatter formatter;
    private final ChatConfig config;

    public MotdService(MessageFormatter formatter, ChatConfig config) {
        this.formatter = formatter;
        this.config = config;
    }

    /** The text to show, or null when the administrator keeps the one of {@code server.properties}. */
    public @Nullable Component motd(MinecraftServer server) {
        if (!config.motdEnabled.get()) {
            return null;
        }
        return formatter.format(Message.plain(ChatMessages.Motd.TEXT), ServerPlaceholderContext.of(server));
    }
}
