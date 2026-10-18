package com.panita.enriquecraft.discord.service;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Reads the frames of an animated GIF the way a browser draws them: each frame is drawn on a canvas of the size of the GIF,
 * after the previous frame has been disposed of as that frame asked. Like every picture from the network it is untrusted, so
 * the size of the canvas and the amount of work are limited before and while frames are read.
 */
public final class GifDecoder {

    /** What is given for each frame: the whole canvas as it looks, and how long it stays. */
    @FunctionalInterface
    public interface FrameConsumer {
        void accept(BufferedImage canvas, int delayMs);
    }

    /** About 4 megapixels, which is more than a GIF that is meant to be seen ever has. */
    public static final long MAX_CANVAS_PIXELS = 4_000_000L;
    /** The most pixels drawn for one GIF, over all its frames. */
    public static final long MAX_TOTAL_PIXELS = 120_000_000L;
    public static final int MAX_FRAMES = 200;

    private GifDecoder() {
    }

    /**
     * Gives the frames in order, at most {@code maxFrames} (the rest of the GIF is left out), as many as the pixel budget allows.
     *
     * @return how many frames were given
     */
    public static int frames(byte[] bytes, int maxFrames, FrameConsumer consumer) throws ImageDecoder.DecodeException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ImageDecoder.DecodeException(ImageDecoder.Failure.UNSUPPORTED, null);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, false);
                return read(reader, maxFrames, consumer);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new ImageDecoder.DecodeException(ImageDecoder.Failure.CORRUPT, e);
        }
    }

    private static int read(ImageReader reader, int maxFrames, FrameConsumer consumer) throws IOException, ImageDecoder.DecodeException {
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        Node screen = child(reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
        if (screen != null) {
            width = Math.max(width, number(screen, "logicalScreenWidth", width));
            height = Math.max(height, number(screen, "logicalScreenHeight", height));
        }
        long canvasPixels = (long) width * height;
        if (canvasPixels <= 0) {
            throw new ImageDecoder.DecodeException(ImageDecoder.Failure.CORRUPT, null);
        }
        if (canvasPixels > MAX_CANVAS_PIXELS) {
            throw new ImageDecoder.DecodeException(ImageDecoder.Failure.TOO_LARGE, null);
        }
        int budget = (int) Math.min(Math.min(maxFrames, MAX_FRAMES), MAX_TOTAL_PIXELS / canvasPixels);
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage beforeFrame = null;
        int[] previousRect = null;
        String previousDisposal = "none";
        int given = 0;
        for (int index = 0; index < budget; index++) {
            BufferedImage frame;
            try {
                frame = reader.read(index);
            } catch (IndexOutOfBoundsException end) {
                break;
            }
            IIOMetadata metadata = reader.getImageMetadata(index);
            Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
            Node descriptor = child(tree, "ImageDescriptor");
            Node control = child(tree, "GraphicControlExtension");
            int left = descriptor == null ? 0 : number(descriptor, "imageLeftPosition", 0);
            int top = descriptor == null ? 0 : number(descriptor, "imageTopPosition", 0);
            String disposal = control == null ? "none" : text(control, "disposalMethod", "none");
            int delay = control == null ? 0 : number(control, "delayTime", 0) * 10;

            // The previous frame is disposed of before this one is drawn
            if ("restoreToBackgroundColor".equals(previousDisposal) && previousRect != null) {
                clear(canvas, previousRect);
            } else if ("restoreToPrevious".equals(previousDisposal) && beforeFrame != null) {
                canvas = copy(beforeFrame);
            }
            beforeFrame = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;

            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.drawImage(frame, left, top, null);
            } finally {
                graphics.dispose();
            }
            consumer.accept(copy(canvas), delay);
            given++;
            previousDisposal = disposal;
            previousRect = new int[]{left, top, frame.getWidth(), frame.getHeight()};
        }
        return given;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static void clear(BufferedImage canvas, int[] rect) {
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(rect[0], rect[1], rect[2], rect[3]);
        } finally {
            graphics.dispose();
        }
    }

    private static Node child(Node parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (name.equals(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }

    private static String text(Node node, String attribute, String fallback) {
        NamedNodeMap attributes = node.getAttributes();
        Node value = attributes == null ? null : attributes.getNamedItem(attribute);
        return value == null ? fallback : value.getNodeValue();
    }

    private static int number(Node node, String attribute, int fallback) {
        try {
            return Integer.parseInt(text(node, attribute, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
