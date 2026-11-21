package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What the whitelist and the op list say about how a name logs in. Their entries carry it in the UUID
 * (see {@link AuthMode#of}), so staff decide it when they add a player and nothing else is stored.
 */
public final class WhitelistModes {

    private WhitelistModes() {
    }

    /**
     * The entry that rules a name according to the whitelist and the ops, or empty when neither lists it.
     * When entries disagree, a premium one wins: it demands real proof of the account, so it is the safer reading.
     */
    public static Optional<NameAndId> listed(PlayerList players, String name) {
        return fromEntries(Stream.concat(
                players.getWhiteList().getEntries().stream().map(entry -> entry.getUser()),
                players.getOps().getEntries().stream().map(entry -> entry.getUser())).toList(), name);
    }

    static Optional<NameAndId> fromEntries(Collection<NameAndId> entries, String name) {
        Collection<NameAndId> matching = entries.stream().filter(entry -> entry.name().equalsIgnoreCase(name)).toList();
        return matching.stream().filter(entry -> AuthMode.of(entry) == AuthMode.PREMIUM).findFirst()
                .or(() -> matching.stream().findFirst());
    }
}
