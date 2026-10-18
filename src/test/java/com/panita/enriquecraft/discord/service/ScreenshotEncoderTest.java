package com.panita.enriquecraft.discord.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotEncoderTest {

    private static byte[] png(int width, int height, boolean noise) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, noise ? 0xFF000000 | random.nextInt(0xFFFFFF) : 0x80FF0000);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void aSmallPictureIsKeptAsItIsAndBecomesAJpeg() throws Exception {
        ScreenshotEncoder.Encoded encoded = ScreenshotEncoder.reencode(png(64, 32, false));
        assertEquals(64, encoded.width());
        assertEquals(32, encoded.height());
        assertEquals((byte) 0xFF, encoded.bytes()[0]);
        assertEquals((byte) 0xD8, encoded.bytes()[1]);
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(encoded.bytes()));
        assertEquals(64, read.getWidth());
    }

    @Test
    void aBigPictureIsFittedToTheLongSide() throws Exception {
        ScreenshotEncoder.Encoded encoded = ScreenshotEncoder.reencode(png(3840, 2160, false));
        assertEquals(ScreenshotEncoder.MAX_SIDE, encoded.width());
        assertEquals(1080, encoded.height());
    }

    @Test
    void theResultAlwaysFitsWhatTheBotTakes() throws Exception {
        ScreenshotEncoder.Encoded encoded = ScreenshotEncoder.reencode(png(1920, 1080, true));
        assertTrue(encoded.bytes().length <= ScreenshotEncoder.MAX_BYTES);
    }

    @Test
    void whatIsNotAPictureIsRefused() {
        assertThrows(ImageDecoder.DecodeException.class, () -> ScreenshotEncoder.reencode("not a picture".getBytes()));
    }
}
