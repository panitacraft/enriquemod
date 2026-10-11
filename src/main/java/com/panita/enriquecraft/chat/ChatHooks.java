package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.chat.service.NicknameService;
import net.minecraft.network.chat.Component;
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

    private ChatHooks(NicknameService nicknames) {
        this.nicknames = nicknames;
    }

    static void install(NicknameService nicknames) {
        instance = new ChatHooks(nicknames);
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
}
