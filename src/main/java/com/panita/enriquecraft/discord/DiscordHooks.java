package com.panita.enriquecraft.discord;

import com.panita.enriquecraft.discord.service.ShownMaps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
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

    /**
     * Whether a packet must be dropped. A player in creative mode can send the map they were shown as an item to put in a slot,
     * which would turn the picture into a real item that fits in an item frame; it is refused, and what the player sees is put
     * back as the server has it.
     */
    public boolean refuses(ServerPlayer player, Packet<?> packet) {
        if (packet instanceof ServerboundSetCreativeModeSlotPacket creative && ShownMaps.isShownMap(creative.itemStack())) {
            player.level().getServer().execute(() -> {
                maps.clear(player);
                player.containerMenu.sendAllDataToRemote();
            });
            return true;
        }
        return false;
    }

    /** A packet of a player is about to be handled on the server thread. */
    public void packetReceived(ServerPlayer player, Packet<?> packet) {
        maps.packetReceived(player, packet);
    }
}
