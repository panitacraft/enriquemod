package com.panita.enriquecraft.discord.protocol;

import java.util.Optional;
import java.util.UUID;

/**
 * A player as the bot is told about them.
 *
 * @param uuid     the UUID of the player
 * @param name     their real name
 * @param nickname what they are called in chat, when they have a nickname
 * @param premium  whether the name belongs to a Microsoft account, which picks the head the bot shows
 */
public record PlayerInfo(UUID uuid, String name, Optional<String> nickname, boolean premium) {
}
