package com.panita.enriquecraft.discord.network;

import java.util.Optional;

/** Where a player asked a screenshot to go. */
public enum ShareDestination {
    CHAT,
    DISCORD,
    BOTH;

    public static Optional<ShareDestination> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? Optional.of(values()[ordinal]) : Optional.empty();
    }

    public boolean toChat() {
        return this != DISCORD;
    }
}
