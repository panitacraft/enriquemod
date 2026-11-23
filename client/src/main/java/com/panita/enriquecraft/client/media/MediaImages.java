package com.panita.enriquecraft.client.media;

import com.panita.enriquecraft.discord.service.GifDecoder;
import com.panita.enriquecraft.discord.service.ImageDecoder;
import com.panita.enriquecraft.discord.service.ImageFetcher;
import com.panita.enriquecraft.discord.service.MediaLinks;
import com.panita.enriquecraft.discord.service.PreviewLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The images of the chat this client may show, and the textures made from them. An image is downloaded only when the player asks to
 * see it (points at its label, or opens it), never because a line arrived, so a chat full of images costs nothing. Only an address
 * the server announced is shown as a preview, and only from a host this client trusts on its own. Everything here runs on the client
 * thread except the download and the scaling, which run on a worker.
 */
public final class MediaImages {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaImages.class);

    /** The texture of a preview has this many pixels for each pixel of the screen, so it stays sharp on a bigger GUI scale. */
    static final int SCALE = 2;
    private static final int MAX_ANNOUNCED = 500;
    private static final int MAX_PREVIEWS = 12;
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    /** Frames of an animation kept for one image, and the pixels all of them may take together (64 MB). */
    private static final int MAX_FRAMES = 60;
    private static final long FRAME_PIXEL_BUDGET = 16L * 1024 * 1024;

    /** How an image of a given size is fitted: the size of its texture, in pixels. */
    private interface Fit {
        Dimension of(int width, int height);
    }

    private record Prepared(int width, int height, List<int[]> frames, List<Integer> delaysMs) {
    }

    /** The addresses the server announced for lines of chat, oldest first. */
    private static final Map<String, Boolean> ANNOUNCED = new LinkedHashMap<>(16, 0.75F, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_ANNOUNCED;
        }
    };

    /** An empty value is a preview still loading, or one that could not be loaded; it is not tried again until the next join. */
    private static final Map<String, Optional<MediaImage>> PREVIEWS = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Optional<MediaImage>> eldest) {
            if (size() <= MAX_PREVIEWS) {
                return false;
            }
            eldest.getValue().ifPresent(MediaImage::close);
            return true;
        }
    };

    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "enriquecraft-media-images");
        thread.setDaemon(true);
        return thread;
    });
    private static final ImageFetcher FETCHER = new ImageFetcher(WORKER, (int) MAX_BYTES, Duration.ofSeconds(8));

    private static int counter;

    private MediaImages() {
    }

    /** The server said the line it just sent links to this image: it may be previewed when its label is pointed at. */
    public static void allow(String url) {
        if (MediaLinks.isTrusted(url)) {
            ANNOUNCED.put(url, Boolean.TRUE);
        }
    }

    /**
     * The preview of an announced image, drawn when its label is pointed at. The first time asks for it, the download starts, and
     * the answer is empty until it is ready.
     */
    static Optional<MediaImage> preview(String url) {
        if (!ANNOUNCED.containsKey(url)) {
            return Optional.empty();
        }
        Optional<MediaImage> known = PREVIEWS.get(url);
        if (known != null) {
            return known;
        }
        PREVIEWS.put(url, Optional.empty());
        load(url, (width, height) -> {
            PreviewLayout.Size size = PreviewLayout.fit(width, height);
            return new Dimension(size.width() * SCALE, size.height() * SCALE);
        }, image -> {
            if (PREVIEWS.containsKey(url)) {
                PREVIEWS.put(url, image);
            } else {
                image.ifPresent(MediaImage::close);
            }
        });
        return Optional.empty();
    }

    /** Opens the viewer on an image the server asked to show. */
    public static void openViewer(String url) {
        if (MediaLinks.isTrusted(url)) {
            Minecraft client = Minecraft.getInstance();
            client.setScreenAndShow(new MediaViewerScreen(url));
        }
    }

    /**
     * Loads an image at the size of the window it is shown in, never made bigger than it is, for the viewer.
     *
     * @param boxWidth  the room the image has, in pixels of the window
     * @param boxHeight the same for the height
     * @param done      called on the client thread, with an empty value when the image could not be loaded; the caller closes the image
     */
    static void loadFull(String url, int boxWidth, int boxHeight, Consumer<Optional<MediaImage>> done) {
        load(url, (width, height) -> {
            double scale = Math.min(1.0, Math.min(boxWidth / (double) width, boxHeight / (double) height));
            return new Dimension(Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale)));
        }, done);
    }

    /** Forgets everything, as the textures belong to the game session that is ending. */
    public static void clear() {
        PREVIEWS.values().forEach(image -> image.ifPresent(MediaImage::close));
        PREVIEWS.clear();
        ANNOUNCED.clear();
    }

    private static void load(String url, Fit fit, Consumer<Optional<MediaImage>> done) {
        Minecraft client = Minecraft.getInstance();
        FETCHER.fetch(URI.create(url))
                .thenApplyAsync(bytes -> prepare(bytes, fit), WORKER)
                .whenComplete((prepared, failure) -> client.execute(() -> {
                    if (failure != null) {
                        LOGGER.debug("Image {} could not be loaded", url, failure);
                        done.accept(Optional.empty());
                    } else {
                        // The texture is made here, as the graphics device belongs to this thread
                        Identifier id = Identifier.fromNamespaceAndPath("enriquecraft", "media_image/" + counter++);
                        done.accept(Optional.of(new MediaImage(client.getTextureManager(), id, prepared.width(), prepared.height(),
                                prepared.frames(), prepared.delaysMs())));
                    }
                }));
    }

    /** On the worker: decode (every frame of a GIF the memory allows, the first of anything else) and scale to the size the fit gives. */
    private static Prepared prepare(byte[] bytes, Fit fit) {
        try {
            List<int[]> frames = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            Dimension[] size = new Dimension[1];
            if (isGif(bytes)) {
                try {
                    GifDecoder.frames(bytes, MAX_FRAMES, (canvas, delay) -> {
                        if (size[0] == null) {
                            size[0] = fit.of(canvas.getWidth(), canvas.getHeight());
                        }
                        // Frames beyond what the memory budget allows are dropped; the animation then loops over the ones kept
                        boolean fits = (frames.size() + 1L) * size[0].width * size[0].height <= FRAME_PIXEL_BUDGET;
                        if (frames.isEmpty() || fits) {
                            frames.add(scale(canvas, size[0]));
                            delays.add(delay);
                        }
                    });
                } catch (ImageDecoder.DecodeException e) {
                    frames.clear();
                    delays.clear();
                    size[0] = null;
                }
            }
            if (frames.isEmpty()) {
                BufferedImage source = ImageDecoder.decode(bytes);
                size[0] = fit.of(source.getWidth(), source.getHeight());
                frames.add(scale(source, size[0]));
                delays.add(0);
            }
            return new Prepared(size[0].width, size[0].height, frames, delays);
        } catch (ImageDecoder.DecodeException e) {
            throw new IllegalStateException(e.failure().name(), e);
        }
    }

    private static boolean isGif(byte[] bytes) {
        return bytes.length > 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
    }

    private static int[] scale(BufferedImage source, Dimension size) {
        BufferedImage scaled = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = scaled.createGraphics();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            canvas.drawImage(source, 0, 0, size.width, size.height, null);
        } finally {
            canvas.dispose();
        }
        return scaled.getRGB(0, 0, scaled.getWidth(), scaled.getHeight(), null, 0, scaled.getWidth());
    }
}
