package com.panita.enriquecraft.discord.service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Reads a picture that came from the network, which is untrusted: its dimensions are checked before anything is decoded,
 * so a small file that unpacks to an enormous picture cannot use up the memory of the server.
 */
public final class ImageDecoder {

    /** Why a picture could not be read. */
    public enum Failure {
        /** No reader for the format, such as WebP, which the JDK does not read. */
        UNSUPPORTED,
        TOO_LARGE,
        CORRUPT
    }

    public static final class DecodeException extends Exception {
        private final Failure failure;

        DecodeException(Failure failure, Throwable cause) {
            super(failure.name(), cause);
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }

    /** About 40 megapixels: far more than a map can show, and about 160 MB once decoded. */
    public static final long MAX_PIXELS = 40_000_000L;

    static {
        // A temporary file for a picture is never worth it
        ImageIO.setUseCache(false);
    }

    private ImageDecoder() {
    }

    /** The first frame of a PNG, JPEG, GIF or BMP. */
    public static BufferedImage decode(byte[] bytes) throws DecodeException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new DecodeException(Failure.UNSUPPORTED, null);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0) {
                    throw new DecodeException(Failure.CORRUPT, null);
                }
                if (pixels > MAX_PIXELS) {
                    throw new DecodeException(Failure.TOO_LARGE, null);
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new DecodeException(Failure.CORRUPT, null);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new DecodeException(Failure.CORRUPT, e);
        }
    }
}
