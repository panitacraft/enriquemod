package com.panita.enriquecraft.client.chat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * An image of the chat as a texture on the graphics device, cut into {@code rows} lines of chat, with the frames to play when it
 * is a GIF. The frames wait as pixels in memory and the next one is copied into the texture only when it is due and the image is
 * being drawn, so an image the chat does not show costs nothing. Used on the client thread only.
 */
final class ChatImage {

    /** Browsers treat a GIF frame that asks for almost no time as a tenth of a second; so does this. */
    private static final int MIN_DELAY_MS = 20;
    private static final int DEFAULT_DELAY_MS = 100;

    private final TextureManager textures;
    private final Identifier id;
    private final int width;
    private final int rows;
    private final DynamicTexture texture;
    private final List<int[]> frames;
    private final List<Integer> delaysMs;
    private int current;
    private long dueAt;

    ChatImage(TextureManager textures, Identifier id, int width, int height, int rows, List<int[]> frames, List<Integer> delaysMs) {
        this.textures = textures;
        this.id = id;
        this.width = width;
        this.rows = rows;
        this.frames = frames;
        this.delaysMs = delaysMs;
        int textureWidth = width * ChatImages.SCALE;
        NativeImage pixels = new NativeImage(textureWidth, height, true);
        fill(pixels, frames.getFirst());
        this.texture = new DynamicTexture(() -> "enriquecraft chat image", pixels);
        textures.register(id, texture);
    }

    Identifier id() {
        return id;
    }

    /** Its width on screen. */
    int width() {
        return width;
    }

    int textureWidth() {
        return texture.getPixels().getWidth();
    }

    int textureHeight() {
        return texture.getPixels().getHeight();
    }

    int rows() {
        return rows;
    }

    /** Shows the frame that is due at this moment, if the image has more than one. */
    void advance(long now) {
        if (frames.size() < 2) {
            return;
        }
        if (dueAt == 0) {
            dueAt = now + delayOf(0);
            return;
        }
        if (now < dueAt) {
            return;
        }
        // A pause (the chat was hidden) is not caught up on: one step, then the delay counts from now
        current = (current + 1) % frames.size();
        dueAt = now + delayOf(current);
        fill(texture.getPixels(), frames.get(current));
        texture.upload();
    }

    void close() {
        textures.release(id);
    }

    private int delayOf(int frame) {
        int delay = delaysMs.get(frame);
        return delay < MIN_DELAY_MS ? DEFAULT_DELAY_MS : delay;
    }

    private static void fill(NativeImage pixels, int[] argb) {
        int w = pixels.getWidth();
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < w; x++) {
                pixels.setPixel(x, y, argb[y * w + x]);
            }
        }
    }
}
