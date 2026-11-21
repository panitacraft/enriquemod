package com.panita.enriquecraft.chat.service;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The real names of everyone who plays here, whether they are online or not: those who are, and everyone on the whitelist,
 * which is every player the server lets in. No nickname can be any of them.
 */
public final class KnownPlayers {

    public Collection<String> namesOn(MinecraftServer server) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            names.add(player.getGameProfile().name());
        }
        for (UserWhiteListEntry entry : server.getPlayerList().getWhiteList().getEntries()) {
            NameAndId user = entry.getUser();
            if (user != null) {
                names.add(user.name());
            }
        }
        return names;
    }
}
