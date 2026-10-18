package com.panita.enriquecraft.discord;

import com.panita.enriquecraft.discord.service.ShownMaps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The way the discord module's mixin reaches its services: one static door, installed once when the module loads. Without it
 * the mixin leaves vanilla behavior untouched.
 */
public final class DiscordHooks {

    private static volatile @Nullable DiscordHooks instance;

    private final ShownMaps maps;

    private DiscordHooks(ShownMaps maps) {
        this.maps = maps;
    }

    static void install(ShownMaps maps) {
        instance = new DiscordHooks(maps);
    }

    public static @Nullable DiscordHooks get() {
        return instance;
    }

    /** A packet of a player is about to be handled on the server thread. */
    public void packetReceived(ServerPlayer player, Packet<?> packet) {
        maps.packetReceived(player, packet);
    }
}
