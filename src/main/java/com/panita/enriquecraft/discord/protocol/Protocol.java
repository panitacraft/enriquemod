package com.panita.enriquecraft.discord.protocol;

import java.util.Set;

/**
 * The constants of the bridge protocol, which the Panita Bot defines (see its {@code docs/minecraft-bridge-plan.md}).
 */
public final class Protocol {

    /** The version of the frames this mod speaks; a bot with another one refuses the connection. */
    public static final int VERSION = 1;
    /** The largest frame either side accepts, in bytes. */
    public static final int MAX_FRAME_BYTES = 16 * 1024;
    /** The longest line of chat that travels over the bridge, in characters. */
    public static final int MAX_CHAT_LENGTH = 256;
    /** The largest server icon the hello carries, in bytes before encoding, so that the frame stays under {@link #MAX_FRAME_BYTES}. */
    public static final int MAX_ICON_BYTES = 10_000;
    /** The optional feature of showing the images of Discord messages, announced in the hello and the welcome. */
    public static final String FEATURE_MEDIA = "media";
    /** The most images of one message that travel in a frame. */
    public static final int MAX_MEDIA = 4;
    public static final int MAX_MEDIA_URL = 1024;
    public static final int MAX_MEDIA_NAME = 100;
    /** The largest image the bot offers, in bytes. */
    public static final long MAX_MEDIA_BYTES = 8L * 1024 * 1024;
    public static final Set<String> MEDIA_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    /** Close code of the bot for a mod it does not speak the same protocol with. */
    public static final int CLOSE_PROTOCOL = 4002;

    private Protocol() {
    }
}
