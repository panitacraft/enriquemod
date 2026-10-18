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
    /** Only in the bot's welcome: it has the upload endpoint, so the mod may offer screenshot sharing. */
    public static final String FEATURE_MEDIA_UPLOAD = "media_upload";
    /** The optional feature of showing what a Discord reply answers, announced in the hello. */
    public static final String FEATURE_REPLY = "reply";
    /** The longest name of the author of the message a reply answers; longer is cut by the bot and refused here. */
    public static final int MAX_REPLY_AUTHOR = 80;
    /** The most images of one message that travel in a frame. */
    public static final int MAX_MEDIA = 4;
    public static final int MAX_MEDIA_URL = 1024;
    public static final int MAX_MEDIA_NAME = 100;
    /** The largest image the bot offers, in bytes. */
    public static final long MAX_MEDIA_BYTES = 8L * 1024 * 1024;
    public static final Set<String> MEDIA_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    /** Whether an image may be linked at this address: always {@code https}, and plain {@code http} only for a local host, for development. */
    public static boolean isSecureOrLocal(java.net.URI address) {
        String host = address.getHost();
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host);
        return "https".equals(address.getScheme()) || ("http".equals(address.getScheme()) && local);
    }

    /** Close code of the bot for a mod it does not speak the same protocol with. */
    public static final int CLOSE_PROTOCOL = 4002;

    private Protocol() {
    }
}
