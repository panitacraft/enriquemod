package com.panita.enriquecraft.discord.protocol;

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
    /** Close code of the bot for a mod it does not speak the same protocol with. */
    public static final int CLOSE_PROTOCOL = 4002;

    private Protocol() {
    }
}
