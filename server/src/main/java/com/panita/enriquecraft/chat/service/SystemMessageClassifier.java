package com.panita.enriquecraft.chat.service;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

/**
 * Recognizes the messages the game broadcasts about a player by the translation key they are built from, and tells which
 * player they are about. A message it does not know, such as one another mod broadcasts, is not recognized.
 */
public final class SystemMessageClassifier {

    public enum Kind { JOINED, JOINED_RENAMED, LEFT, DIED, ADVANCED }

    /**
     * @param playerName the real name of the player the message is about
     * @param oldName    the name the player had before, for a join under a new name
     * @param message    the game's own text, which the client translates
     */
    public record Recognized(Kind kind, String playerName, Optional<String> oldName, Component message) {
    }

    static final String JOINED = "multiplayer.player.joined";
    static final String JOINED_RENAMED = "multiplayer.player.joined.renamed";
    static final String LEFT = "multiplayer.player.left";
    private static final String DEATH_PREFIX = "death.";
    private static final String ADVANCEMENT_PREFIX = "chat.type.advancement.";

    public Optional<Recognized> recognize(Component message) {
        if (!(message.getContents() instanceof TranslatableContents translatable)) {
            return Optional.empty();
        }
        Optional<String> playerName = playerNameOf(translatable);
        Optional<Kind> kind = kindOf(translatable.getKey());
        if (playerName.isEmpty() || kind.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> oldName = kind.get() == Kind.JOINED_RENAMED ? oldNameOf(translatable) : Optional.empty();
        return Optional.of(new Recognized(kind.get(), playerName.get(), oldName, message));
    }

    private static Optional<Kind> kindOf(String key) {
        return switch (key) {
            case JOINED -> Optional.of(Kind.JOINED);
            case JOINED_RENAMED -> Optional.of(Kind.JOINED_RENAMED);
            case LEFT -> Optional.of(Kind.LEFT);
            default -> {
                if (key.startsWith(DEATH_PREFIX)) {
                    yield Optional.of(Kind.DIED);
                }
                yield key.startsWith(ADVANCEMENT_PREFIX) ? Optional.of(Kind.ADVANCED) : Optional.empty();
            }
        };
    }

    /**
     * The first argument of these messages is the name of the player, written the way the game writes it: with their real
     * name as the text a click on it would insert, even when it shows a nickname.
     */
    private static Optional<String> playerNameOf(TranslatableContents message) {
        Object[] arguments = message.getArgs();
        if (arguments.length > 0 && arguments[0] instanceof Component name) {
            return Optional.ofNullable(name.getStyle().getInsertion());
        }
        return Optional.empty();
    }

    /** The name the player had before, which the game gives as text. */
    private static Optional<String> oldNameOf(TranslatableContents message) {
        Object[] arguments = message.getArgs();
        return arguments.length > 1 ? Optional.of(String.valueOf(arguments[1])) : Optional.empty();
    }
}
