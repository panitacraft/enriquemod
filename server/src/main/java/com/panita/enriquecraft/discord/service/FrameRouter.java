package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.InFrame;
import net.minecraft.server.MinecraftServer;

import java.util.function.BiConsumer;

/**
 * Takes what the bot sends, which arrives on a network thread, to the server thread and to whoever handles it. The handlers
 * are set after the connection exists, so the two can be built in any order.
 */
public final class FrameRouter {

    private final ServerInfo info;
    private volatile BiConsumer<MinecraftServer, InFrame.Chat> chat = (server, frame) -> {
    };
    private volatile BiConsumer<MinecraftServer, InFrame.Query> query = (server, frame) -> {
    };

    public FrameRouter(ServerInfo info) {
        this.info = info;
    }

    public void onChat(BiConsumer<MinecraftServer, InFrame.Chat> handler) {
        chat = handler;
    }

    public void onQuery(BiConsumer<MinecraftServer, InFrame.Query> handler) {
        query = handler;
    }

    /** Hands a frame to its handler on the server thread; a frame that arrives while no server runs is dropped. */
    public void route(InFrame frame) {
        info.server().ifPresent(server -> server.execute(() -> {
            switch (frame) {
                case InFrame.Chat message -> chat.accept(server, message);
                case InFrame.Query question -> query.accept(server, question);
                case InFrame.Welcome welcome -> {
                }
                case InFrame.Error error -> {
                }
            }
        }));
    }
}
