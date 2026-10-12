package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.OutFrame;

/**
 * Where the game's frames go, seen from the code that makes them. The connection is the real one; tests use a stand-in.
 */
public interface FrameSink {

    /** Passes a frame to the bot, or holds it for a little while if the line is down. Never blocks and never throws. */
    void send(OutFrame frame);

    /** Whether the bot bridges this chat channel, as far as the server knows. */
    boolean bridges(String channel);
}
