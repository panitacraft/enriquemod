package com.panita.enriquecraft.discord.service;

/**
 * How big an image is in the chat of a client that shows them, and how many lines it takes. Pure arithmetic, so it is the same
 * for the code that loads the image and the code that reserves its place.
 */
public final class ChatImageLayout {

    /** The height of a line of chat, in GUI pixels. */
    public static final int LINE_HEIGHT = 9;
    public static final int MAX_WIDTH = 200;
    public static final int MAX_HEIGHT = 12 * LINE_HEIGHT;

    /** The size an image is drawn at, and the number of lines of chat it takes. */
    public record Size(int width, int height, int lines) {
    }

    private ChatImageLayout() {
    }

    /** The image fitted into the room the chat gives it, keeping its proportions and never made bigger than it is. */
    public static Size fit(int sourceWidth, int sourceHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("an image has a size");
        }
        double scale = Math.min(1.0, Math.min(MAX_WIDTH / (double) sourceWidth, MAX_HEIGHT / (double) sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new Size(width, height, (height + LINE_HEIGHT - 1) / LINE_HEIGHT);
    }
}
