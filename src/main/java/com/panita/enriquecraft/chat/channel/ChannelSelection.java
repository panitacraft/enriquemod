package com.panita.enriquecraft.chat.channel;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The channel each player writes in. Nothing is stored: a player who has not chosen, or has left, writes in the default
 * channel, and so does one whose channel was removed from the config or is no longer open to them. Used from the server
 * thread only.
 */
public final class ChannelSelection {

    private final ChannelRegistry channels;
    private final Map<UUID, String> chosen = new HashMap<>();

    public ChannelSelection(ChannelRegistry channels) {
        this.channels = channels;
    }

    public ChannelDefinition current(ServerPlayer player) {
        String id = chosen.get(player.getUUID());
        return channels.find(id == null ? "" : id)
                .filter(channel -> channel.canUse(player))
                .orElseGet(channels::defaultChannel);
    }

    public void select(ServerPlayer player, ChannelDefinition channel) {
        chosen.put(player.getUUID(), channel.id());
    }

    public void forget(UUID player) {
        chosen.remove(player);
    }
}
