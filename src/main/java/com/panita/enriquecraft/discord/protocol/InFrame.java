package com.panita.enriquecraft.discord.protocol;

import java.util.List;

/**
 * A frame the bot sends to the server. Whatever it says has been through {@link FrameCodec}, which checked its shape.
 */
public sealed interface InFrame {

    /**
     * The answer to the hello.
     *
     * @param channels the chat channels of the server the bot bridges
     * @param features the optional features the bot offers, such as {@link Protocol#FEATURE_MEDIA}
     */
    record Welcome(int protocol, List<String> channels, List<String> features) implements InFrame {

        public Welcome {
            channels = List.copyOf(channels);
            features = List.copyOf(features);
        }
    }

    /**
     * A message of a linked Discord member for a chat channel.
     *
     * @param discordId     the Discord account of the author
     * @param ign           their Minecraft name, which is what the message is shown under
     * @param text          what they wrote; empty only when there are images
     * @param media         the images that passed the shape checks, at most {@link Protocol#MAX_MEDIA}
     * @param rejectedMedia how many entries were left out because they were invalid or too many
     */
    record Chat(String channel, String discordId, String ign, String text, List<MediaEntry> media, int rejectedMedia) implements InFrame {

        public Chat {
            media = List.copyOf(media);
        }
    }

    /** A question about the server, to be answered with a reply of the same id. */
    record Query(String id, QueryKind what) implements InFrame {
    }

    /** The bot is unhappy about something it received; it closes the connection right after. */
    record Error(String code, String message) implements InFrame {
    }
}
