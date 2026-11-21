package com.panita.enriquecraft.discord.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Makes a screenshot that a client sent into one the server is willing to pass on. Decoding it and writing a new JPEG is what
 * makes it safe: whatever else the file held (metadata, extra data after the picture, another format in disguise) is left
 * behind, and the size is the server's decision, not the client's.
 */
public final class ScreenshotEncoder {

    /** A screenshot re-encoded, with the size it ended up with. */
    public record Encoded(byte[] bytes, int width, int height) {
    }

    /** The long side of what is passed on: enough to read a screen, small enough to travel. */
    public static final int MAX_SIDE = 1920;
    /** What the bot accepts is 2 MiB; this leaves room for the rest of the request. */
    public static final int MAX_BYTES = 2 * 1024 * 1024 - 16 * 1024;
    private static final float[] QUALITIES = {0.85F, 0.7F, 0.55F, 0.4F};

    private ScreenshotEncoder() {
    }

    /** @throws ImageDecoder.DecodeException when the bytes are not a picture the server reads, or are too large to decode */
    public static Encoded reencode(byte[] bytes) throws ImageDecoder.DecodeException {
        BufferedImage source = ImageDecoder.decode(bytes);
        BufferedImage picture = fit(source);
        try {
            for (float quality : QUALITIES) {
                byte[] jpeg = jpeg(picture, quality);
                if (jpeg.length <= MAX_BYTES) {
                    return new Encoded(jpeg, picture.getWidth(), picture.getHeight());
                }
            }
        } catch (IOException e) {
            throw new ImageDecoder.DecodeException(ImageDecoder.Failure.CORRUPT, e);
        }
        throw new ImageDecoder.DecodeException(ImageDecoder.Failure.TOO_LARGE, null);
    }

    /** The picture on a white background (JPEG has no transparency), no bigger than {@link #MAX_SIDE}. */
    private static BufferedImage fit(BufferedImage source) {
        double scale = Math.min(1.0, MAX_SIDE / (double) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = picture.createGraphics();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            canvas.setColor(Color.WHITE);
            canvas.fillRect(0, 0, width, height);
            canvas.drawImage(source, 0, 0, width, height, null);
        } finally {
            canvas.dispose();
        }
        return picture;
    }

    private static byte[] jpeg(BufferedImage picture, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(quality);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(picture, null, null), parameters);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
