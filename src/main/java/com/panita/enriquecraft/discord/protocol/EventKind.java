package com.panita.enriquecraft.discord.protocol;

/**
 * What the bot is told happened in the game. The first five are about a player.
 */
public enum EventKind {
    JOIN("join", true),
    FIRST_JOIN("first_join", true),
    LEAVE("leave", true),
    DEATH("death", true),
    ADVANCEMENT("advancement", true),
    BROADCAST("broadcast", false),
    STARTED("started", false),
    STOPPING("stopping", false);

    private final String id;
    private final boolean aboutPlayer;

    EventKind(String id, boolean aboutPlayer) {
        this.id = id;
        this.aboutPlayer = aboutPlayer;
    }

    public String id() {
        return id;
    }

    public boolean aboutPlayer() {
        return aboutPlayer;
    }
}
