package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.mixin.MinecraftServerStatusAccessor;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * The text the server list shows under the name of the server. The game rebuilds its status every few seconds (while it is running)
 * and asks for the text each time, so what it says about the server, such as how many players are online, stays current.
 */
public final class MotdService {

    private final MessageFormatter formatter;
    private final ChatConfig config;

    public MotdService(MessageFormatter formatter, ChatConfig config) {
        this.formatter = formatter;
        this.config = config;
    }

    /**
     * Builds the status of the server list again now, so a changed text or setting shows without waiting for the game, which only
     * does it from its tick: every few seconds, and not at all while the server is paused for having no players. Also needed to go
     * back to the text of {@code server.properties} when the setting is turned off.
     */
    public void refresh(MinecraftServer server) {
        MinecraftServerStatusAccessor status = (MinecraftServerStatusAccessor) server;
        status.enriquecraft$setStatus(status.enriquecraft$buildServerStatus());
    }

    /** The text to show, or null when the administrator keeps the one of {@code server.properties}. */
    public @Nullable Component motd(MinecraftServer server) {
        if (!config.motdEnabled.get()) {
            return null;
        }
        return formatter.format(Message.plain(ChatMessages.Motd.TEXT), ServerPlaceholderContext.of(server));
    }
}
