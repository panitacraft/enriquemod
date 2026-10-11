package com.panita.enriquecraft.chat.service;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tells every player's client the name to show for a player in the tab list, after it changed. The name itself is
 * what the game asks the player for when it builds the entry.
 */
public final class TabListNames {

    public void refresh(ServerPlayer player) {
        player.level().getServer().getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
    }
}
