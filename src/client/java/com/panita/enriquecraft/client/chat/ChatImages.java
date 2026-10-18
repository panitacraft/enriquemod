package com.panita.enriquecraft.client.chat;

import com.panita.enriquecraft.discord.service.ChatImageLayout;
import com.panita.enriquecraft.discord.service.GifDecoder;
import com.panita.enriquecraft.discord.service.ImageDecoder;
import com.panita.enriquecraft.discord.service.ImageFetcher;
import com.panita.enriquecraft.discord.service.MediaLinks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * The images the server announced for lines of chat, and the textures made from them. Only an address the server announced is
 * ever fetched, and only from a host this client trusts on its own. Everything here runs on the client thread except the
 * download and the scaling, which run on a worker. A line of chat that links to a ready image gets lines that hold slices of
 * it, under its text.
 */
public final class ChatImages {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatImages.class);

    /** The texture has this many pixels for each pixel of the screen, so the image stays sharp on a bigger GUI scale. */
    static final int SCALE = 2;
    private static final int MAX_TEXTURES = 24;
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    /** Frames of an animation kept for one image, which bounds the memory a GIF may use (about 16 MB of pixels). */
    private static final int MAX_FRAMES = 60;

    private record Prepared(String url, int width, int height, int rows, List<int[]> frames, List<Integer> delaysMs) {
    }

    /** An empty value is an image still loading, or one that could not be loaded; it is not tried again until the next join. */
    private static final Map<String, Optional<ChatImage>> IMAGES = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Optional<ChatImage>> eldest) {
            if (size() <= MAX_TEXTURES) {
                return false;
            }
            eldest.getValue().ifPresent(ChatImage::close);
            return true;
        }
    };

    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "enriquecraft-chat-images");
        thread.setDaemon(true);
        return thread;
    });
    private static final ImageFetcher FETCHER = new ImageFetcher(WORKER, (int) MAX_BYTES, Duration.ofSeconds(8));

    private static int counter;

    private ChatImages() {
    }

    /** The server said the line it just sent links to this image: start loading it, and redraw the chat when it is ready. */
    public static void expect(String url) {
        if (IMAGES.containsKey(url) || !ChatImageSettings.enabled() || !MediaLinks.isTrusted(url)) {
            return;
        }
        IMAGES.put(url, Optional.empty());
        Minecraft client = Minecraft.getInstance();
        FETCHER.fetch(URI.create(url))
                .thenApplyAsync(bytes -> prepare(url, bytes), WORKER)
                .whenComplete((prepared, failure) -> client.execute(() -> {
                    if (failure != null) {
                        LOGGER.debug("Chat image {} could not be loaded", url, failure);
                    } else if (IMAGES.containsKey(url)) {
                        show(client, prepared);
                    }
                }));
    }

    /** Forgets everything, as the textures belong to the game session that is ending. */
    public static void clear() {
        IMAGES.values().forEach(image -> image.ifPresent(ChatImage::close));
        IMAGES.clear();
    }

    /** The lines to add under a line of chat: a slice for each row of each of the images it links to that are ready. */
    public static List<FormattedCharSequence> linesFor(Component message) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String url : linksOf(message)) {
            Optional<ChatImage> image = IMAGES.getOrDefault(url, Optional.empty());
            image.ifPresent(ready -> {
                for (int row = 0; row < ready.rows(); row++) {
                    lines.add(new ChatImageLine(ready, row));
                }
            });
        }
        return lines;
    }

    private static List<String> linksOf(Component message) {
        List<String> links = new ArrayList<>();
        message.visit((Style style, String text) -> {
            if (style.getClickEvent() instanceof ClickEvent.OpenUrl open) {
                String url = open.uri().toString();
                if (!links.contains(url)) {
                    links.add(url);
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return links;
    }

    /** On the worker: decode (every frame of a GIF, the first of anything else), fit into the room the chat gives an image and scale once. */
    private static Prepared prepare(String url, byte[] bytes) {
        try {
            List<int[]> frames = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            ChatImageLayout.Size[] size = new ChatImageLayout.Size[1];
            if (isGif(bytes)) {
                try {
                    GifDecoder.frames(bytes, MAX_FRAMES, (canvas, delay) -> {
                        if (size[0] == null) {
                            size[0] = ChatImageLayout.fit(canvas.getWidth(), canvas.getHeight());
                        }
                        frames.add(scale(canvas, size[0]));
                        delays.add(delay);
                    });
                } catch (ImageDecoder.DecodeException e) {
                    frames.clear();
                    delays.clear();
                    size[0] = null;
                }
            }
            if (frames.isEmpty()) {
                BufferedImage source = ImageDecoder.decode(bytes);
                size[0] = ChatImageLayout.fit(source.getWidth(), source.getHeight());
                frames.add(scale(source, size[0]));
                delays.add(0);
            }
            return new Prepared(url, size[0].width(), size[0].lines() * ChatImageLayout.LINE_HEIGHT * SCALE, size[0].lines(), frames, delays);
        } catch (ImageDecoder.DecodeException e) {
            throw new IllegalStateException(e.failure().name(), e);
        }
    }

    private static boolean isGif(byte[] bytes) {
        return bytes.length > 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
    }

    private static int[] scale(BufferedImage source, ChatImageLayout.Size size) {
        BufferedImage scaled = new BufferedImage(size.width() * SCALE, size.lines() * ChatImageLayout.LINE_HEIGHT * SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = scaled.createGraphics();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            canvas.drawImage(source, 0, 0, size.width() * SCALE, size.height() * SCALE, null);
        } finally {
            canvas.dispose();
        }
        return scaled.getRGB(0, 0, scaled.getWidth(), scaled.getHeight(), null, 0, scaled.getWidth());
    }

    /** On the client thread: the texture is made here, as the graphics device belongs to it. */
    private static void show(Minecraft client, Prepared prepared) {
        Identifier id = Identifier.fromNamespaceAndPath("enriquecraft", "chat_image/" + counter++);
        ChatImage image = new ChatImage(client.getTextureManager(), id, prepared.width(), prepared.height(), prepared.rows(),
                prepared.frames(), prepared.delaysMs());
        IMAGES.put(prepared.url(), Optional.of(image));
        // The line was drawn before its image was ready, so the chat lays its lines out again
        client.gui.hud.getChat().rescaleChat();
    }
}
