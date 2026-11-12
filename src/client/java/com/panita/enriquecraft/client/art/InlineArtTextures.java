package com.panita.enriquecraft.client.art;

import com.mojang.blaze3d.platform.NativeImage;
import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.core.art.ArtLinks;
import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.discord.service.ImageDecoder;
import com.panita.enriquecraft.discord.service.ImageFetcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The images the server announced for glyphs of the chat, and the textures made from them. Only an address the server announced is
 * ever fetched, and only from a host this client trusts on its own. The download and the scaling run on a worker, the texture is made
 * on the client thread, which owns the graphics device.
 * <p>
 * The glyph of an image is the one character of the font {@code enriquecraft:art/<id>}; the client's font code asks here for it
 * (see {@code FontManagerGlyphMixin}). Images are kept until the player leaves the server: they are small, and the server announces
 * each one only once.
 */
public final class InlineArtTextures {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlineArtTextures.class);

    /** The height of a glyph in GUI pixels, the height of the text. Its texture has one pixel per screen pixel, so nothing is resampled. */
    private static final int HEIGHT = 8;
    private static final int MAX_WIDTH = 64;
    /** Pixels this transparent or more count as empty when the margins of an image are cut. */
    private static final int MARGIN_ALPHA = 8;
    private static final int MAX_IMAGES = 256;
    /** A fetch that fails is tried again a few times, as the network may only have hiccuped; the server announces an image once. */
    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_SECONDS = 4;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    /** The longest side of the picture an item is drawn as: emblems are shown at 32 pixels at most, at any GUI scale. */
    static final int ICON_SIDE = 128;

    /** What the client knows about the image of one glyph. */
    private static final class Entry {
        final InlineArtGlyph glyph = new InlineArtGlyph();
        boolean requested;
        int attempts;
        Identifier texture;
        Identifier iconTexture;
        /** The image without its transparent margins, kept to make the glyph again when the GUI scale changes. */
        BufferedImage trimmed;
        /** The GUI scale the glyph texture was made for. */
        int scale;
        boolean rebuilding;
    }

    private record Pixels(int width, int height, int[] argb) {
    }

    private record Prepared(Pixels glyph, Pixels icon, BufferedImage trimmed, int scale) {
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "enriquecraft-inline-art");
        thread.setDaemon(true);
        return thread;
    });
    private static final ImageFetcher FETCHER = new ImageFetcher(WORKER, MAX_BYTES, Duration.ofSeconds(8));

    private InlineArtTextures() {
    }

    /** The glyphs of a font of this mod's images, when the description is one; the client's font code calls this for every font it draws. */
    public static Optional<GlyphSource> sourceFor(FontDescription description) {
        if (description instanceof FontDescription.Resource resource && Enriquecraft.MOD_ID.equals(resource.id().getNamespace())
                && resource.id().getPath().startsWith(InlineArts.FONT_PATH)) {
            String id = resource.id().getPath().substring(InlineArts.FONT_PATH.length());
            if (ArtLinks.isId(id)) {
                Entry entry = entry(id);
                refit(id, entry);
                return Optional.of(entry.glyph);
            }
        }
        return Optional.empty();
    }

    private static String rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    /** The picture an item marked with this id is drawn as, once it is ready. */
    public static Optional<Identifier> iconTexture(String id) {
        Entry entry = ENTRIES.get(id);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.iconTexture);
    }

    /** The server said the next lines hold this image: start loading it. Anything that does not add up is ignored. */
    public static void expect(String id, String url) {
        if (!ArtLinks.isId(id) || !ArtLinks.isTrusted(url) || !ArtLinks.idOf(url).equals(id)) {
            return;
        }
        Entry entry = entry(id);
        if (entry.requested || ENTRIES.size() > MAX_IMAGES) {
            return;
        }
        entry.requested = true;
        entry.attempts++;
        Minecraft client = Minecraft.getInstance();
        int scale = guiScale(client);
        FETCHER.fetch(URI.create(url))
                .thenApplyAsync(bytes -> prepare(bytes, scale), WORKER)
                .whenComplete((prepared, failure) -> client.execute(() -> {
                    if (failure != null) {
                        LOGGER.warn("The art {} ({}) could not be loaded (attempt {}): {}", id, url, entry.attempts,
                                rootCause(failure));
                        if (entry.attempts < MAX_ATTEMPTS) {
                            entry.requested = false;
                            CompletableFuture.delayedExecutor(RETRY_SECONDS, TimeUnit.SECONDS).execute(() -> client.execute(() -> expect(id, url)));
                        }
                    } else if (ENTRIES.get(id) == entry) {
                        show(client, id, entry, prepared);
                    }
                }));
    }

    /** Forgets everything, as the textures belong to the game session that is ending. */
    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        ENTRIES.values().forEach(entry -> {
            if (entry.texture != null) {
                client.getTextureManager().release(entry.texture);
            }
            if (entry.iconTexture != null) {
                client.getTextureManager().release(entry.iconTexture);
            }
        });
        ENTRIES.values().forEach(entry -> entry.glyph.hide());
        ENTRIES.clear();
    }

    private static Entry entry(String id) {
        return ENTRIES.computeIfAbsent(id, key -> new Entry());
    }

    private static int guiScale(Minecraft client) {
        return Math.max(1, client.getWindow().getGuiScale());
    }

    /**
     * On the worker: decode, then scale once for the glyph, from the image without its transparent margins, and once to the picture of
     * an item, which keeps them. Both keep the proportions.
     */
    private static Prepared prepare(byte[] bytes, int scale) {
        try {
            BufferedImage source = ImageDecoder.decode(bytes);
            BufferedImage trimmed = trim(source);
            float fit = Math.min(1.0F, (float) ICON_SIDE / Math.max(source.getWidth(), source.getHeight()));
            int iconWidth = Math.max(1, Math.round(source.getWidth() * fit));
            int iconHeight = Math.max(1, Math.round(source.getHeight() * fit));
            return new Prepared(glyphPixels(trimmed, scale), scale(source, ICON_SIDE, ICON_SIDE, iconWidth, iconHeight,
                    (ICON_SIDE - iconWidth) / 2, (ICON_SIDE - iconHeight) / 2), trimmed, scale);
        } catch (ImageDecoder.DecodeException e) {
            throw new IllegalStateException(e.failure().name(), e);
        }
    }

    /** The glyph's pixels: as tall as the text is on screen, so one texture pixel is one screen pixel at this GUI scale. */
    private static Pixels glyphPixels(BufferedImage trimmed, int scale) {
        int height = HEIGHT * scale;
        int width = Math.max(1, Math.min(MAX_WIDTH * scale, Math.round(trimmed.getWidth() * (float) height / trimmed.getHeight())));
        return scale(trimmed, width, height, width, height, 0, 0);
    }

    /** The image cut to the box that holds everything visible; the image itself when nothing is, or nothing can be cut. */
    private static BufferedImage trim(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getRGB(x, y) >>> 24) >= MARGIN_ALPHA) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0 || (minX == 0 && minY == 0 && maxX == source.getWidth() - 1 && maxY == source.getHeight() - 1)) {
            return source;
        }
        BufferedImage cut = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = cut.createGraphics();
        try {
            canvas.drawImage(source, -minX, -minY, null);
        } finally {
            canvas.dispose();
        }
        return cut;
    }

    /** The GUI scale changed since the glyph's texture was made: make it again for the new one, so it stays one pixel to one pixel. */
    private static void refit(String id, Entry entry) {
        Minecraft client = Minecraft.getInstance();
        int scale = guiScale(client);
        if (entry.trimmed == null || entry.rebuilding || entry.scale == scale) {
            return;
        }
        entry.rebuilding = true;
        BufferedImage trimmed = entry.trimmed;
        CompletableFuture.supplyAsync(() -> glyphPixels(trimmed, scale), WORKER).whenComplete((pixels, failure) -> client.execute(() -> {
            entry.rebuilding = false;
            if (failure == null && ENTRIES.get(id) == entry) {
                showGlyph(client, id, entry, pixels, scale);
            }
        }));
    }

    /** The image drawn at the size given, at that place of a canvas that is transparent around it. */
    private static Pixels scale(BufferedImage source, int width, int height, int drawnWidth, int drawnHeight, int x, int y) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = scaled.createGraphics();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            canvas.drawImage(source, x, y, drawnWidth, drawnHeight, null);
        } finally {
            canvas.dispose();
        }
        return new Pixels(width, height, scaled.getRGB(0, 0, width, height, null, 0, width));
    }

    private static Identifier register(Minecraft client, String path, Pixels pixels) {
        NativeImage image = new NativeImage(pixels.width(), pixels.height(), true);
        for (int y = 0; y < pixels.height(); y++) {
            for (int x = 0; x < pixels.width(); x++) {
                image.setPixel(x, y, pixels.argb()[y * pixels.width() + x]);
            }
        }
        Identifier texture = Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path);
        client.getTextureManager().register(texture, new DynamicTexture(() -> "enriquecraft inline art", image));
        return texture;
    }

    /** On the client thread: the textures are made here, and the glyph starts drawing its. */
    private static void show(Minecraft client, String id, Entry entry, Prepared prepared) {
        entry.trimmed = prepared.trimmed();
        entry.iconTexture = register(client, "inline_art/" + id + "_icon", prepared.icon());
        showGlyph(client, id, entry, prepared.glyph(), prepared.scale());
    }

    private static void showGlyph(Minecraft client, String id, Entry entry, Pixels pixels, int scale) {
        if (entry.texture != null) {
            client.getTextureManager().release(entry.texture);
        }
        entry.scale = scale;
        entry.texture = register(client, "inline_art/" + id + "_x" + scale, pixels);
        entry.glyph.show(entry.texture, pixels.width() / (float) scale);
        // The lines that hold the glyph were laid out before it had its width
        client.gui.hud.getChat().rescaleChat();
    }
}
