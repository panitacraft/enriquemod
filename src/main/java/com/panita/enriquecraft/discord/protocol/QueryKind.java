package com.panita.enriquecraft.discord.protocol;

import java.util.Arrays;
import java.util.Optional;

/**
 * What the bot may ask the server.
 */
public enum QueryKind {
    PLAYERLIST("playerlist"),
    TPS("tps"),
    STATUS("status"),
    PLAYERINFO("playerinfo");

    private final String id;

    QueryKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<QueryKind> of(String id) {
        return Arrays.stream(values()).filter(kind -> kind.id.equals(id)).findFirst();
    }
}
