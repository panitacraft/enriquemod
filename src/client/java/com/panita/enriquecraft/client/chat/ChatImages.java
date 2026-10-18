package com.panita.enriquecraft.client.chat;

import com.panita.enriquecraft.discord.service.ChatImageLayout;
import com.panita.enriquecraft.discord.service.ImageDecoder;
import com.panita.enriquecraft.discord.service.ImageFetcher;
import com.panita.enriquecraft.discord.service.MediaLinks;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
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

    /** A texture ready to draw: {@code width} is its width on screen, and it is cut into {@code rows} lines of chat. */
    record Ready(Identifier id, int width, int textureWidth, int textureHeight, int rows) {
    }

    private record Prepared(String url, int width, int height, int rows, int[] pixels) {
    }

    /** An empty value is an image still loading, or one that could not be loaded; it is not tried again until the next join. */
    private static final Map<String, Optional<Ready>> IMAGES = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Optional<Ready>> eldest) {
            if (size() <= MAX_TEXTURES) {
                return false;
            }
            eldest.getValue().ifPresent(ChatImages::release);
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
        IMAGES.values().forEach(image -> image.ifPresent(ChatImages::release));
        IMAGES.clear();
    }

    /** The lines to add under a line of chat: a slice for each row of each of the images it links to that are ready. */
    public static List<FormattedCharSequence> linesFor(Component message) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String url : linksOf(message)) {
            Optional<Ready> image = IMAGES.getOrDefault(url, Optional.empty());
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

    /** On the worker: decode, fit into the room the chat gives an image and scale once, so drawing is only a copy. */
    private static Prepared prepare(String url, byte[] bytes) {
        try {
            BufferedImage source = ImageDecoder.decode(bytes);
            ChatImageLayout.Size size = ChatImageLayout.fit(source.getWidth(), source.getHeight());
            BufferedImage scaled = new BufferedImage(size.width() * SCALE, size.lines() * ChatImageLayout.LINE_HEIGHT * SCALE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = scaled.createGraphics();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                canvas.drawImage(source, 0, 0, size.width() * SCALE, size.height() * SCALE, null);
            } finally {
                canvas.dispose();
            }
            return new Prepared(url, size.width(), scaled.getHeight(), size.lines(),
                    scaled.getRGB(0, 0, scaled.getWidth(), scaled.getHeight(), null, 0, scaled.getWidth()));
        } catch (ImageDecoder.DecodeException e) {
            throw new IllegalStateException(e.failure().name(), e);
        }
    }

    /** On the client thread: the texture is made here, as the graphics device belongs to it. */
    private static void show(Minecraft client, Prepared prepared) {
        int textureWidth = prepared.width() * SCALE;
        NativeImage pixels = new NativeImage(textureWidth, prepared.height(), false);
        for (int y = 0; y < prepared.height(); y++) {
            for (int x = 0; x < textureWidth; x++) {
                pixels.setPixel(x, y, prepared.pixels()[y * textureWidth + x]);
            }
        }
        Identifier id = Identifier.fromNamespaceAndPath("enriquecraft", "chat_image/" + counter++);
        client.getTextureManager().register(id, new DynamicTexture(() -> "enriquecraft chat image", pixels));
        IMAGES.put(prepared.url(), Optional.of(new Ready(id, prepared.width(), textureWidth, prepared.height(), prepared.rows())));
        // The line was drawn before its image was ready, so the chat lays its lines out again
        client.gui.hud.getChat().rescaleChat();
    }

    private static void release(Ready image) {
        Minecraft.getInstance().getTextureManager().release(image.id());
    }
}
