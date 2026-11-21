package com.panita.enriquecraft.discord.service;

import java.util.List;

/**
 * What is shown on a map: one picture, or the frames of an animation that repeat. Each frame is the
 * {@link MapPixels#SIZE} squared bytes of a map.
 *
 * @param delaysMs how long each frame stays, in milliseconds; as many as frames
 */
public record MapAnimation(List<byte[]> frames, List<Integer> delaysMs) {

    public MapAnimation {
        if (frames.isEmpty() || frames.size() != delaysMs.size()) {
            throw new IllegalArgumentException("an animation needs a delay for each of its frames");
        }
        frames = List.copyOf(frames);
        delaysMs = List.copyOf(delaysMs);
    }

    public static MapAnimation still(byte[] frame) {
        return new MapAnimation(List.of(frame), List.of(0));
    }

    public boolean animated() {
        return frames.size() > 1;
    }
}
