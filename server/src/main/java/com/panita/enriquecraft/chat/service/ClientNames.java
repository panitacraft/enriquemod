package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.chat.mixin.ChunkMapAccessor;
import com.panita.enriquecraft.chat.mixin.TrackedEntityInvoker;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Makes the clients learn the name of a player again after it changed. A client reads the name from the player's entry in
 * its list when the entity of the player appears, so the entry is replaced and the entity is shown again, as if the player had
 * just come into view. What the clients are told is {@code PlayerInfoEntryMixin}'s doing.
 */
public final class ClientNames {

    public void refresh(ServerPlayer player) {
        List<ServerPlayer> everyone = player.level().getServer().getPlayerList().getPlayers();
        List<ServerPlayer> others = everyone.stream().filter(other -> other != player).toList();
        Object tracked = ((ChunkMapAccessor) player.level().getChunkSource().chunkMap).enriquecraft$trackedEntities().get(player.getId());

        if (tracked != null) {
            others.forEach(viewer -> ((TrackedEntityInvoker) tracked).enriquecraft$stopShowingTo(viewer));
        }
        ClientboundPlayerInfoRemovePacket remove = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        ClientboundPlayerInfoUpdatePacket add = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
        everyone.forEach(viewer -> {
            viewer.connection.send(remove);
            viewer.connection.send(add);
        });
        if (tracked != null) {
            others.forEach(viewer -> ((TrackedEntityInvoker) tracked).enriquecraft$showToIfInView(viewer));
        }
    }
}
