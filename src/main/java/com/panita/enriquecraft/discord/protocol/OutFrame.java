package com.panita.enriquecraft.discord.protocol;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A frame the server sends to the bot.
 */
public sealed interface OutFrame {

    /**
     * The first frame of a connection.
     *
     * @param startedAt when the server started
     * @param icon      the server's icon as PNG bytes, when it has one that fits in the frame
     * @param features  the optional features this mod can use, such as {@link Protocol#FEATURE_MEDIA}
     */
    record Hello(int protocol, String modVersion, String minecraftVersion, Instant startedAt, int maxPlayers,
                 Optional<byte[]> icon, List<String> features) implements OutFrame {

        public Hello {
            features = List.copyOf(features);
        }
    }

    /** A line a player wrote in a chat channel; {@code emote} is what they did with {@code /me}. */
    record Chat(String channel, PlayerInfo player, String text, boolean emote) implements OutFrame {
    }

    /**
     * Something that happened in the game.
     *
     * @param player the player it is about, for the kinds that are
     * @param text   the game's own English text of a death, the title of an advancement, or the message of a broadcast
     * @param key    the game's translation key, so the text can be translated later
     */
    record Event(EventKind kind, Optional<PlayerInfo> player, Optional<String> text, Optional<String> key) implements OutFrame {
    }

    /** The answer to a query: the data when it went well, a code of what went wrong otherwise. */
    record Reply(String id, Optional<JsonObject> data, Optional<String> error) implements OutFrame {

        public static Reply ok(String id, JsonObject data) {
            return new Reply(id, Optional.of(data), Optional.empty());
        }

        public static Reply failed(String id, String error) {
            return new Reply(id, Optional.empty(), Optional.of(error));
        }
    }
}
