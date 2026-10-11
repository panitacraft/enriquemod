package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.chat.service.SystemMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The way the chat module's mixins reach its services. A mixin is instantiated by Mixin, not by the service registry,
 * so it cannot receive services through a constructor; this is the one static door, installed once when the module
 * loads. Without it every mixin leaves vanilla behavior untouched.
 */
public final class ChatHooks {

    private static volatile @Nullable ChatHooks instance;

    private final NicknameService nicknames;
    private final SystemMessages systemMessages;

    private ChatHooks(NicknameService nicknames, SystemMessages systemMessages) {
        this.nicknames = nicknames;
        this.systemMessages = systemMessages;
    }

    static void install(NicknameService nicknames, SystemMessages systemMessages) {
        instance = new ChatHooks(nicknames, systemMessages);
    }

    public static @Nullable ChatHooks get() {
        return instance;
    }

    /** The nickname of a player, or null when they have none or are not a player of this server. */
    public @Nullable Component nicknameOf(Player player) {
        return player instanceof ServerPlayer
                ? nicknames.nicknameOf(player.getUUID()).orElse(null)
                : null;
    }

    public void joinBegins(ServerPlayer player) {
        systemMessages.joinBegins(player);
    }

    public void joinEnds() {
        systemMessages.joinEnds();
    }

    /** A message the game is about to broadcast to everyone; true when the module took it over and the game must not send it. */
    public boolean systemMessage(MinecraftServer server, Component message) {
        return systemMessages.handle(server, message);
    }
}
