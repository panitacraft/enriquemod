package com.panita.enriquecraft.discord.service;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shows a picture to one player on a map in their hand without giving them anything. The server's inventory is never
 * touched: the client is told that the slot they hold has a filled map, and told the colors of that map. The map is not
 * saved in the world and cannot be taken. As soon as the player does anything but move or chat, or after a while, the slot is
 * sent again as it really is.
 * <p>
 * Everything here runs on the server thread.
 */
public final class ShownMaps {

    /** A map id that real maps never reach, so the client's own maps are not replaced. */
    private static final MapId ID = new MapId(0x7FFFFF00);

    /** What a player's client sends all the time or while typing: none of it means they are doing something else. */
    private static final List<Class<? extends Packet<?>>> RESTING = List.of(
            ServerboundMovePlayerPacket.class,
            ServerboundPlayerInputPacket.class,
            ServerboundPlayerCommandPacket.class,
            ServerboundPlayerAbilitiesPacket.class,
            ServerboundKeepAlivePacket.class,
            ServerboundPongPacket.class,
            ServerboundClientTickEndPacket.class,
            ServerboundClientInformationPacket.class,
            ServerboundAcceptTeleportationPacket.class,
            ServerboundChunkBatchReceivedPacket.class,
            ServerboundPlayerLoadedPacket.class,
            ServerboundChatPacket.class,
            ServerboundChatAckPacket.class,
            ServerboundChatSessionUpdatePacket.class,
            ServerboundChatCommandPacket.class,
            ServerboundChatCommandSignedPacket.class,
            ServerboundCommandSuggestionPacket.class,
            ServerboundCustomPayloadPacket.class);

    private static final class Shown {
        private final int slot;
        private final Instant expires;
        private final MapAnimation animation;
        private int frame;
        private Instant nextFrame;

        private Shown(int slot, Instant expires, MapAnimation animation, Instant nextFrame) {
            this.slot = slot;
            this.expires = expires;
            this.animation = animation;
            this.nextFrame = nextFrame;
        }
    }

    private final Supplier<Instant> clock;
    private final Map<UUID, Shown> shown = new HashMap<>();

    public ShownMaps(Supplier<Instant> clock) {
        this.clock = clock;
    }

    /** Whether a packet is one the client sends while a player only moves or writes in the chat. */
    public static boolean isResting(Class<?> packet) {
        return RESTING.stream().anyMatch(resting -> resting.isAssignableFrom(packet));
    }

    /**
     * Shows a picture on a map in the slot the player holds, for at most {@code lifetime}. An animation repeats and, after its
     * first frame, only what changes from one frame to the next is sent.
     */
    public void show(ServerPlayer player, String name, MapAnimation animation, Duration lifetime) {
        int slot = player.getInventory().getSelectedSlot();
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, ID);
        map.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        player.connection.send(new ClientboundMapItemDataPacket(ID, (byte) 0, true, List.of(),
                new MapItemSavedData.MapPatch(0, 0, MapPixels.SIZE, MapPixels.SIZE, animation.frames().get(0))));
        player.connection.send(new ClientboundSetPlayerInventoryPacket(slot, map));
        Instant now = clock.get();
        shown.put(player.getUUID(), new Shown(slot, now.plus(lifetime), animation, now.plusMillis(animation.delaysMs().get(0))));
    }

    /** The smallest rectangle that holds every byte that differs, or nothing when the two are the same. */
    static Optional<MapItemSavedData.MapPatch> changes(byte[] before, byte[] after) {
        int minX = MapPixels.SIZE;
        int minY = MapPixels.SIZE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < MapPixels.SIZE; y++) {
            for (int x = 0; x < MapPixels.SIZE; x++) {
                if (before[y * MapPixels.SIZE + x] != after[y * MapPixels.SIZE + x]) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return Optional.empty();
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        byte[] colors = new byte[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(after, (minY + row) * MapPixels.SIZE + minX, colors, row * width, width);
        }
        return Optional.of(new MapItemSavedData.MapPatch(minX, minY, width, height, colors));
    }

    private void advance(ServerPlayer player, Shown state, Instant now) {
        MapAnimation animation = state.animation;
        byte[] before = animation.frames().get(state.frame);
        state.frame = (state.frame + 1) % animation.frames().size();
        // A server that fell behind does not play the missed frames in a burst
        state.nextFrame = now.plusMillis(animation.delaysMs().get(state.frame));
        changes(before, animation.frames().get(state.frame)).ifPresent(patch ->
                player.connection.send(new ClientboundMapItemDataPacket(ID, (byte) 0, true, List.of(), patch)));
    }

    /**
     * Whether a stack is the map shown by {@link #show}. It exists only on the client, so a client that offers one to the server
     * (a player in creative mode can move any item of their inventory screen into a slot) is not telling the truth.
     */
    public static boolean isShownMap(ItemStack stack) {
        return stack.is(Items.FILLED_MAP) && ID.equals(stack.get(DataComponents.MAP_ID));
    }

    /** Puts the slot back as it is, if a map is being shown to the player. */
    public void clear(ServerPlayer player) {
        Shown was = shown.remove(player.getUUID());
        if (was != null) {
            player.connection.send(new ClientboundSetPlayerInventoryPacket(was.slot, player.getInventory().getItem(was.slot).copy()));
        }
    }

    /** A packet of the player arrived: anything that is not just moving or chatting ends the map. */
    public void packetReceived(ServerPlayer player, Packet<?> packet) {
        if (shown.containsKey(player.getUUID()) && !isResting(packet.getClass())) {
            clear(player);
        }
    }

    /** The player left; there is nothing to send. */
    public void forget(UUID player) {
        shown.remove(player);
    }

    /** Ends the maps that ran out, whose slot is no longer the held one, or whose player opened a container. */
    public void tick(MinecraftServer server) {
        if (shown.isEmpty()) {
            return;
        }
        Instant now = clock.get();
        for (Map.Entry<UUID, Shown> entry : List.copyOf(shown.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                shown.remove(entry.getKey());
            } else if (entry.getValue().expires.isBefore(now) || player.getInventory().getSelectedSlot() != entry.getValue().slot
                    || player.containerMenu != player.inventoryMenu) {
                clear(player);
            } else if (entry.getValue().animation.animated() && !now.isBefore(entry.getValue().nextFrame)) {
                advance(player, entry.getValue(), now);
            }
        }
    }
}
