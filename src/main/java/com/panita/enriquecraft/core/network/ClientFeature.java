package com.panita.enriquecraft.core.network;

import java.util.Arrays;
import java.util.Optional;

/**
 * An enhancement the client companion can offer on top of the vanilla experience. The client
 * announces the ones it implements; the server only ever uses an enhancement the player announced.
 * <p>
 * Features travel as their {@link #id()}, never as an ordinal, so a client that knows a feature the
 * server does not (or the reverse) is handled by ignoring it.
 */
public enum ClientFeature {

    /** Server-described screens with buttons and inputs, instead of the vanilla chest. */
    CUSTOM_UI("custom_ui");

    private final String id;

    ClientFeature(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<ClientFeature> byId(String id) {
        return Arrays.stream(values()).filter(feature -> feature.id.equals(id)).findFirst();
    }
}
