package com.panita.enriquecraft.discord.service;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GifDecoderTest {

    /** One frame of a GIF to write: where it goes on the canvas, how long it stays, and how it is disposed of. */
    private record Frame(BufferedImage image, int left, int top, int delayCentiseconds, String disposal) {
    }

    private static BufferedImage solid(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static byte[] gif(int canvasWidth, int canvasHeight, List<Frame> frames) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            IIOMetadata stream = writer.getDefaultStreamMetadata(param);
            String streamFormat = stream.getNativeMetadataFormatName();
            IIOMetadataNode streamRoot = (IIOMetadataNode) stream.getAsTree(streamFormat);
            IIOMetadataNode screen = child(streamRoot, "LogicalScreenDescriptor");
            screen.setAttribute("logicalScreenWidth", Integer.toString(canvasWidth));
            screen.setAttribute("logicalScreenHeight", Integer.toString(canvasHeight));
            stream.setFromTree(streamFormat, streamRoot);
            writer.prepareWriteSequence(stream);
            for (Frame frame : frames) {
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frame.image()), param);
                String format = metadata.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
                IIOMetadataNode control = child(root, "GraphicControlExtension");
                control.setAttribute("delayTime", Integer.toString(frame.delayCentiseconds()));
                control.setAttribute("disposalMethod", frame.disposal());
                control.setAttribute("transparentColorFlag", "TRUE");
                control.setAttribute("transparentColorIndex", "0");
                IIOMetadataNode descriptor = child(root, "ImageDescriptor");
                descriptor.setAttribute("imageLeftPosition", Integer.toString(frame.left()));
                descriptor.setAttribute("imageTopPosition", Integer.toString(frame.top()));
                metadata.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(frame.image(), null, metadata), param);
            }
            writer.endWriteSequence();
        }
        writer.dispose();
        return bytes.toByteArray();
    }

    private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
        for (int index = 0; index < parent.getLength(); index++) {
            if (name.equals(parent.item(index).getNodeName())) {
                return (IIOMetadataNode) parent.item(index);
            }
        }
        IIOMetadataNode created = new IIOMetadataNode(name);
        parent.appendChild(created);
        return created;
    }

    private record Played(List<BufferedImage> canvases, List<Integer> delays) {
    }

    private static Played play(byte[] bytes, int maxFrames) throws Exception {
        List<BufferedImage> canvases = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        GifDecoder.frames(bytes, maxFrames, (canvas, delay) -> {
            canvases.add(canvas);
            delays.add(delay);
        });
        return new Played(canvases, delays);
    }

    @Test
    void givesTheFramesInOrderWithTheirDelays() throws Exception {
        byte[] bytes = gif(40, 30, List.of(
                new Frame(solid(40, 30, Color.RED), 0, 0, 10, "none"),
                new Frame(solid(40, 30, Color.GREEN), 0, 0, 20, "none"),
                new Frame(solid(40, 30, Color.BLUE), 0, 0, 30, "none")));

        Played played = play(bytes, 100);

        assertEquals(List.of(100, 200, 300), played.delays());
        assertEquals(3, played.canvases().size());
        assertEquals(40, played.canvases().get(0).getWidth());
        assertEquals(Color.RED.getRGB(), played.canvases().get(0).getRGB(5, 5));
        assertEquals(Color.GREEN.getRGB(), played.canvases().get(1).getRGB(5, 5));
        assertEquals(Color.BLUE.getRGB(), played.canvases().get(2).getRGB(5, 5));
    }

    @Test
    void aSmallFrameIsDrawnOnTheCanvasWhereItBelongsAndTheRestStays() throws Exception {
        byte[] bytes = gif(40, 30, List.of(
                new Frame(solid(40, 30, Color.RED), 0, 0, 10, "none"),
                new Frame(solid(10, 10, Color.GREEN), 20, 10, 10, "none")));

        Played played = play(bytes, 100);

        assertEquals(Color.RED.getRGB(), played.canvases().get(1).getRGB(5, 5), "what was there stays");
        assertEquals(Color.GREEN.getRGB(), played.canvases().get(1).getRGB(25, 15), "the small frame is where it was placed");
        assertEquals(Color.RED.getRGB(), played.canvases().get(1).getRGB(35, 25));
    }

    @Test
    void aFrameThatAsksToBeClearedBeforeTheNextOneLeavesNoTrace() throws Exception {
        byte[] bytes = gif(40, 30, List.of(
                new Frame(solid(40, 30, Color.RED), 0, 0, 10, "none"),
                new Frame(solid(10, 10, Color.GREEN), 20, 10, 10, "restoreToBackgroundColor"),
                new Frame(solid(10, 10, Color.BLUE), 0, 0, 10, "none")));

        Played played = play(bytes, 100);

        assertEquals(Color.GREEN.getRGB(), played.canvases().get(1).getRGB(25, 15));
        assertEquals(0, played.canvases().get(2).getRGB(25, 15) >>> 24, "the green square was cleared to transparent");
        assertEquals(Color.BLUE.getRGB(), played.canvases().get(2).getRGB(5, 5));
        assertEquals(Color.RED.getRGB(), played.canvases().get(2).getRGB(35, 25), "only the rectangle of that frame was cleared");
    }

    @Test
    void aFrameThatAsksToBeUndoneGivesBackWhatWasBeforeIt() throws Exception {
        byte[] bytes = gif(40, 30, List.of(
                new Frame(solid(40, 30, Color.RED), 0, 0, 10, "none"),
                new Frame(solid(10, 10, Color.GREEN), 20, 10, 10, "restoreToPrevious"),
                new Frame(solid(10, 10, Color.BLUE), 0, 0, 10, "none")));

        Played played = play(bytes, 100);

        assertEquals(Color.GREEN.getRGB(), played.canvases().get(1).getRGB(25, 15));
        assertEquals(Color.RED.getRGB(), played.canvases().get(2).getRGB(25, 15), "the red under the green came back");
    }

    @Test
    void stopsAtTheLimitOfFrames() throws Exception {
        List<Frame> frames = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            frames.add(new Frame(solid(8, 8, new Color(index * 30, 0, 0)), 0, 0, 10, "none"));
        }

        assertEquals(3, play(gif(8, 8, frames), 3).canvases().size());
    }

    @Test
    void aSingleFrameGifGivesOneFrame() throws Exception {
        assertEquals(1, play(gif(8, 8, List.of(new Frame(solid(8, 8, Color.RED), 0, 0, 0, "none"))), 100).canvases().size());
    }

    @Test
    void refusesACanvasThatIsTooBigBeforeDrawingAnything() throws Exception {
        byte[] bytes = gif(3000, 3000, List.of(new Frame(solid(8, 8, Color.RED), 0, 0, 10, "none")));

        ImageDecoder.DecodeException error = assertThrows(ImageDecoder.DecodeException.class, () -> play(bytes, 100));

        assertEquals(ImageDecoder.Failure.TOO_LARGE, error.failure());
    }

    @Test
    void whatIsNotAGifIsNotAnimated() {
        assertThrows(ImageDecoder.DecodeException.class, () -> play("not a gif at all".getBytes(), 100));
        assertTrue(true);
    }
}
