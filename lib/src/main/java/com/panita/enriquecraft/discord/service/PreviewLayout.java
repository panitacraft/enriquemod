package com.panita.enriquecraft.discord.service;

/**
 * How big the preview of an image is when a player points at its label in the chat of a client companion. Pure arithmetic, so it is
 * the same for the code that loads the image and the code that draws it.
 */
public final class PreviewLayout {

    public static final int MAX_WIDTH = 200;
    public static final int MAX_HEIGHT = 150;

    /** The size a preview is drawn at, in GUI pixels. */
    public record Size(int width, int height) {
    }

    private PreviewLayout() {
    }

    /** The image fitted into the room of a preview, keeping its proportions and never made bigger than it is. */
    public static Size fit(int sourceWidth, int sourceHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("an image has a size");
        }
        double scale = Math.min(1.0, Math.min(MAX_WIDTH / (double) sourceWidth, MAX_HEIGHT / (double) sourceHeight));
        return new Size(Math.max(1, (int) Math.round(sourceWidth * scale)), Math.max(1, (int) Math.round(sourceHeight * scale)));
    }
}
