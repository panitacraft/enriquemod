package com.panita.enriquecraft.core.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows which enhancements each connected player's client offers. Every feature that has a
 * client-side version asks {@link #supports} before using it, and falls back to the vanilla
 * behavior when the answer is no. A player who never announced anything is vanilla.
 * <p>
 * What a client announces is untrusted: it only counts when it speaks exactly this protocol
 * version, and only for features this server knows.
 */
public final class ClientCapabilities {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCapabilities.class);

    private final Map<UUID, Set<ClientFeature>> features = new ConcurrentHashMap<>();

    /**
     * Decides what a client may use from what it announced.
     *
     * @return the announced features this server knows, or an empty set when the protocol version differs
     */
    static Set<ClientFeature> negotiate(int protocolVersion, List<String> featureIds) {
        Set<ClientFeature> accepted = EnumSet.noneOf(ClientFeature.class);
        if (protocolVersion != NetworkProtocol.VERSION) {
            return accepted;
        }
        featureIds.forEach(id -> ClientFeature.byId(id).ifPresent(accepted::add));
        return accepted;
    }

    /** Records what a player's client announced, replacing anything announced before. */
    public void accept(UUID player, HelloC2S hello) {
        Set<ClientFeature> accepted = negotiate(hello.protocolVersion(), hello.featureIds());
        if (hello.protocolVersion() != NetworkProtocol.VERSION) {
            LOGGER.info("Client protocol {} does not match {}; treating {} as vanilla",
                    hello.protocolVersion(), NetworkProtocol.VERSION, player);
        }
        features.put(player, Collections.unmodifiableSet(accepted));
    }

    /** Forgets a player, so a later connection starts as vanilla again. */
    public void forget(UUID player) {
        features.remove(player);
    }

    public boolean supports(UUID player, ClientFeature feature) {
        return features.getOrDefault(player, Set.of()).contains(feature);
    }
}
