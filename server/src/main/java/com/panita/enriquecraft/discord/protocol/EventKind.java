package com.panita.enriquecraft.discord.protocol;

/**
 * What the bot is told happened in the game. The first eight are about a player. {@code afk} and {@code back} are only sent when the
 * bot offers {@link Protocol#FEATURE_AFK}, and {@code coordinates} when it offers {@link Protocol#FEATURE_COORDINATES}; its text is
 * the position, such as {@code X: 10 Y: 64 Z: -20 (Overworld)}.
 */
public enum EventKind {
    JOIN("join", true),
    FIRST_JOIN("first_join", true),
    LEAVE("leave", true),
    DEATH("death", true),
    ADVANCEMENT("advancement", true),
    AFK("afk", true),
    BACK("back", true),
    COORDINATES("coordinates", true),
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
