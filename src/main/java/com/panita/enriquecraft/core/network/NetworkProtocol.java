package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.Enriquecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What the client companion and the server must agree on before exchanging custom payloads.
 */
public final class NetworkProtocol {

    /**
     * Bumped on every change to a payload's layout. The server treats a client with a different
     * version as vanilla, so a mismatch degrades to the vanilla experience instead of breaking.
     */
    public static final int VERSION = 1;

    private NetworkProtocol() {
    }

    /** The type of a payload; its channel id is {@code enriquecraft:<path>}. */
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path));
    }
}
