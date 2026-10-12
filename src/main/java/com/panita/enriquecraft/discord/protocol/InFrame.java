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
     */
    record Welcome(int protocol, List<String> channels) implements InFrame {

        public Welcome {
            channels = List.copyOf(channels);
        }
    }

    /**
     * A message of a linked Discord member for a chat channel.
     *
     * @param discordId the Discord account of the author
     * @param ign       their Minecraft name, which is what the message is shown under
     */
    record Chat(String channel, String discordId, String ign, String text) implements InFrame {
    }

    /** A question about the server, to be answered with a reply of the same id. */
    record Query(String id, QueryKind what) implements InFrame {
    }

    /** The bot is unhappy about something it received; it closes the connection right after. */
    record Error(String code, String message) implements InFrame {
    }
}
