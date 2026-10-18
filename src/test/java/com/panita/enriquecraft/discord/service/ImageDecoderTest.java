package com.panita.enriquecraft.discord.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageDecoderTest {

    private static byte[] encoded(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);
        return bytes.toByteArray();
    }

    private static ImageDecoder.Failure failureOf(byte[] bytes) {
        return assertThrows(ImageDecoder.DecodeException.class, () -> ImageDecoder.decode(bytes)).failure();
    }

    @Test
    void readsPngJpegAndGif() throws Exception {
        for (String format : new String[]{"png", "jpg", "gif"}) {
            BufferedImage image = ImageDecoder.decode(encoded(format, 40, 30));

            assertEquals(40, image.getWidth(), format);
            assertEquals(30, image.getHeight(), format);
        }
    }

    @Test
    void aFormatWithNoReaderIsUnsupported() {
        byte[] webp = ("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ").getBytes(StandardCharsets.ISO_8859_1);

        assertEquals(ImageDecoder.Failure.UNSUPPORTED, failureOf(webp));
        assertEquals(ImageDecoder.Failure.UNSUPPORTED, failureOf("not an image at all".getBytes(StandardCharsets.UTF_8)));
        assertEquals(ImageDecoder.Failure.UNSUPPORTED, failureOf(new byte[0]));
    }

    @Test
    void aTruncatedPictureIsCorrupt() throws IOException {
        byte[] png = encoded("png", 200, 200);

        assertEquals(ImageDecoder.Failure.CORRUPT, failureOf(java.util.Arrays.copyOf(png, png.length / 2)));
    }

    @Test
    void aSmallFileThatClaimsAHugePictureIsRefusedBeforeItIsDecoded() throws IOException {
        byte[] png = encoded("png", 10, 10);
        ByteBuffer header = ByteBuffer.wrap(png);
        // The IHDR chunk starts after the 8 bytes of signature and 4 of length: type (4), width, height, ...
        header.putInt(16, 20_000);
        header.putInt(20, 20_000);
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        header.putInt(29, (int) crc.getValue());

        assertEquals(ImageDecoder.Failure.TOO_LARGE, failureOf(png));
    }
}
