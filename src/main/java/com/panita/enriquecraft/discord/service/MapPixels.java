package com.panita.enriquecraft.discord.service;

import net.minecraft.world.level.material.MapColor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a picture into the 128x128 bytes of a vanilla map: the picture is fitted without distortion and every pixel becomes
 * the nearest of the colors a map can show. A pixel with no color is left clear (0).
 */
public final class MapPixels {

    public static final int SIZE = 128;
    private static final int TRANSPARENT_BELOW = 128;
    /** The palette is searched through a 5 bits per channel table, which is as exact as a map can show. */
    private static final int LEVELS = 32;

    private static final int[] PALETTE_RGB;
    private static final byte[] PALETTE_ID;
    private static final short[] NEAREST = new short[LEVELS * LEVELS * LEVELS];

    static {
        System.setProperty("java.awt.headless", System.getProperty("java.awt.headless", "true"));
        List<int[]> colors = new ArrayList<>();
        for (int id = 1; id < 62; id++) {
            MapColor color = MapColor.byId(id);
            for (MapColor.Brightness brightness : MapColor.Brightness.values()) {
                colors.add(new int[]{color.calculateARGBColor(brightness) & 0xFFFFFF, color.getPackedId(brightness)});
            }
        }
        PALETTE_RGB = colors.stream().mapToInt(color -> color[0]).toArray();
        PALETTE_ID = new byte[colors.size()];
        for (int index = 0; index < PALETTE_ID.length; index++) {
            PALETTE_ID[index] = (byte) colors.get(index)[1];
        }
        for (int cell = 0; cell < NEAREST.length; cell++) {
            int red = ((cell >> 10) & 31) * 255 / 31;
            int green = ((cell >> 5) & 31) * 255 / 31;
            int blue = (cell & 31) * 255 / 31;
            NEAREST[cell] = (short) closest(red, green, blue);
        }
    }

    private MapPixels() {
    }

    private static int closest(int red, int green, int blue) {
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int index = 0; index < PALETTE_RGB.length; index++) {
            int rgb = PALETTE_RGB[index];
            long dr = red - ((rgb >> 16) & 255);
            long dg = green - ((rgb >> 8) & 255);
            long db = blue - (rgb & 255);
            // The eye is most sensitive to green and least to blue
            long distance = 2 * dr * dr + 4 * dg * dg + 3 * db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = index;
            }
        }
        return best;
    }

    private static int paletteIndex(int red, int green, int blue) {
        return NEAREST[((red >> 3) << 10) | ((green >> 3) << 5) | (blue >> 3)];
    }

    /** The map bytes of the colors the map shows for an exact color, for tests and callers that need one pixel. */
    public static byte colorOf(int rgb) {
        return PALETTE_ID[paletteIndex((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)];
    }

    /**
     * @param dither spreads the error of each pixel to its neighbors, which looks closer to the picture with so few colors
     * @return {@link #SIZE} x {@link #SIZE} bytes, row by row
     */
    public static byte[] fit(BufferedImage image, boolean dither) {
        double scale = Math.min(SIZE / (double) image.getWidth(), SIZE / (double) image.getHeight());
        int width = Math.max(1, Math.min(SIZE, (int) Math.round(image.getWidth() * scale)));
        int height = Math.max(1, Math.min(SIZE, (int) Math.round(image.getHeight() * scale)));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        byte[] map = new byte[SIZE * SIZE];
        int left = (SIZE - width) / 2;
        int top = (SIZE - height) / 2;
        float[][] error = new float[3][3 * (width + 2)];
        float[][] next = new float[3][3 * (width + 2)];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = scaled.getRGB(x, y);
                if ((argb >>> 24) < TRANSPARENT_BELOW) {
                    continue;
                }
                int red = adjusted(argb >> 16, error[0][x + 1], dither);
                int green = adjusted(argb >> 8, error[1][x + 1], dither);
                int blue = adjusted(argb, error[2][x + 1], dither);
                int index = paletteIndex(red, green, blue);
                map[(top + y) * SIZE + left + x] = PALETTE_ID[index];
                if (dither) {
                    int chosen = PALETTE_RGB[index];
                    spread(error, next, x + 1, 0, red - ((chosen >> 16) & 255));
                    spread(error, next, x + 1, 1, green - ((chosen >> 8) & 255));
                    spread(error, next, x + 1, 2, blue - (chosen & 255));
                }
            }
            for (int channel = 0; channel < 3; channel++) {
                float[] done = error[channel];
                error[channel] = next[channel];
                next[channel] = done;
                java.util.Arrays.fill(next[channel], 0f);
            }
        }
        return map;
    }

    private static int adjusted(int channel, float error, boolean dither) {
        int value = (channel & 255) + (dither ? Math.round(error) : 0);
        return Math.max(0, Math.min(255, value));
    }

    /** Floyd-Steinberg: the error goes to the pixel on the right and to the three below. */
    private static void spread(float[][] current, float[][] below, int column, int channel, int error) {
        current[channel][column + 1] += error * 7f / 16f;
        below[channel][column - 1] += error * 3f / 16f;
        below[channel][column] += error * 5f / 16f;
        below[channel][column + 1] += error * 1f / 16f;
    }
}
