package com.panita.enriquecraft.client.share;

import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareEndC2S;
import com.panita.enriquecraft.discord.service.ScreenshotEncoder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Prepares a screenshot the player chose to share (downscaled and encoded off the game thread) and sends it to the server in
 * pieces, a few each tick so that a big one does not flood the connection. The server checks all of it again; this only keeps
 * what is sent small.
 */
final class ScreenshotSender {

    private static final int PIECES_PER_TICK = 8;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "enriquecraft-screenshot");
        thread.setDaemon(true);
        return thread;
    });

    private static final Deque<CustomPacketPayload> QUEUE = new ArrayDeque<>();
    private static int nextId;

    private ScreenshotSender() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (int sent = 0; sent < PIECES_PER_TICK && !QUEUE.isEmpty(); sent++) {
                if (client.player == null || !ClientPlayNetworking.canSend(ShareChunkC2S.TYPE)) {
                    QUEUE.clear();
                    return;
                }
                ClientPlayNetworking.send(QUEUE.poll());
            }
        });
    }

    /** Called on the client thread. */
    static void send(Path file, String caption) {
        Minecraft client = Minecraft.getInstance();
        WORKER.execute(() -> {
            try {
                ScreenshotEncoder.Encoded encoded = ScreenshotEncoder.reencode(Files.readAllBytes(file));
                client.execute(() -> enqueue(encoded.bytes(), caption));
            } catch (IOException | com.panita.enriquecraft.discord.service.ImageDecoder.DecodeException e) {
                client.execute(() -> client.gui.hud.getChat().addClientSystemMessage(Component.translatable("enriquecraft.share.failed")));
            }
        });
    }

    private static void enqueue(byte[] bytes, String caption) {
        if (!ClientPlayNetworking.canSend(ShareBeginC2S.TYPE)) {
            return;
        }
        int id = nextId++;
        QUEUE.add(new ShareBeginC2S(id, bytes.length, caption));
        for (int offset = 0, index = 0; offset < bytes.length; offset += ShareChunkC2S.MAX_BYTES, index++) {
            QUEUE.add(new ShareChunkC2S(id, index, Arrays.copyOfRange(bytes, offset, Math.min(bytes.length, offset + ShareChunkC2S.MAX_BYTES))));
        }
        QUEUE.add(new ShareEndC2S(id));
    }
}
