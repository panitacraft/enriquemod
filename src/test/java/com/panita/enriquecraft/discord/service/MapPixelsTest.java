package com.panita.enriquecraft.discord.service;

import net.minecraft.world.level.material.MapColor;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPixelsTest {

    private static BufferedImage solid(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static byte at(byte[] map, int x, int y) {
        return map[y * MapPixels.SIZE + x];
    }

    @Test
    void whiteBecomesTheBrightestSnowOfTheMap() {
        assertEquals(MapColor.SNOW.getPackedId(MapColor.Brightness.HIGH), MapPixels.colorOf(0xFFFFFF));
    }

    @Test
    void aSquarePictureFillsTheWholeMap() {
        byte[] map = MapPixels.fit(solid(300, 300, Color.RED), false);

        assertEquals(MapPixels.SIZE * MapPixels.SIZE, map.length);
        for (byte pixel : map) {
            assertTrue(pixel != 0);
        }
    }

    @Test
    void aWidePictureIsFittedAndCenteredWithoutDistortion() {
        byte[] map = MapPixels.fit(solid(256, 128, Color.RED), false);

        // 2:1 becomes 128x64: 32 clear rows above and below
        assertEquals(0, at(map, 64, 0));
        assertEquals(0, at(map, 64, 31));
        assertTrue(at(map, 64, 32) != 0);
        assertTrue(at(map, 0, 64) != 0);
        assertTrue(at(map, 127, 95) != 0);
        assertEquals(0, at(map, 64, 96));
        assertEquals(0, at(map, 64, 127));
    }

    @Test
    void aTallPictureIsCenteredSideways() {
        byte[] map = MapPixels.fit(solid(64, 128, Color.BLUE), false);

        assertEquals(0, at(map, 0, 64));
        assertEquals(0, at(map, 31, 64));
        assertTrue(at(map, 32, 64) != 0);
        assertTrue(at(map, 95, 64) != 0);
        assertEquals(0, at(map, 96, 64));
    }

    @Test
    void aPictureOfOnePixelStillShows() {
        byte[] map = MapPixels.fit(solid(1, 1, Color.GREEN), false);

        assertTrue(at(map, 64, 64) != 0);
    }

    @Test
    void transparentPixelsStayClear() {
        byte[] map = MapPixels.fit(solid(64, 64, new Color(255, 0, 0, 0)), true);

        for (byte pixel : map) {
            assertEquals(0, pixel);
        }
    }

    @Test
    void nothingIsEverTheClearColorWhereThePictureIsOpaque() {
        BufferedImage gradient = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                gradient.setRGB(x, y, new Color(x * 2, y * 2, 255 - x * 2).getRGB());
            }
        }

        for (byte pixel : MapPixels.fit(gradient, true)) {
            assertTrue(((pixel & 0xFF) >> 2) != 0, "the base color 0 means clear");
        }
    }

    @Test
    void ditheringBlendsMoreColorsOnAGradientThanPlainMatching() {
        BufferedImage gradient = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                gradient.setRGB(x, y, new Color(x * 2, x * 2, x * 2).getRGB());
            }
        }

        assertTrue(distinct(MapPixels.fit(gradient, true)) >= distinct(MapPixels.fit(gradient, false)));
    }

    private static int distinct(byte[] map) {
        Set<Byte> colors = new HashSet<>();
        for (byte pixel : map) {
            colors.add(pixel);
        }
        return colors.size();
    }
}
