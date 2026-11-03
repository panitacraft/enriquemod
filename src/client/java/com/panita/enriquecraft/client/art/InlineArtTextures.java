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

    /** The texture has this many pixels for each pixel of the screen, so the image stays sharp on a bigger GUI scale. */
    private static final int SCALE = 4;
    /** The height of a glyph in screen pixels, the height of the text. */
    private static final int HEIGHT = 8;
    private static final int MAX_WIDTH = 64;
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
    }

    private record Pixels(int width, int height, int[] argb) {
    }

    private record Prepared(Pixels glyph, Pixels icon) {
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
                return Optional.of(entry(id).glyph);
            }
        }
        return Optional.empty();
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
        FETCHER.fetch(URI.create(url))
                .thenApplyAsync(InlineArtTextures::prepare, WORKER)
                .whenComplete((prepared, failure) -> client.execute(() -> {
                    if (failure != null) {
                        LOGGER.warn("The art {} ({}) could not be loaded (attempt {}): {}", id, url, entry.attempts,
                                String.valueOf(failure.getCause() != null ? failure.getCause() : failure));
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

    /** On the worker: decode, then scale once to the height of a glyph and once to the picture of an item, keeping the proportions. */
    private static Prepared prepare(byte[] bytes) {
        try {
            BufferedImage source = ImageDecoder.decode(bytes);
            int height = HEIGHT * SCALE;
            int width = Math.max(1, Math.min(MAX_WIDTH * SCALE, Math.round(source.getWidth() * (float) height / source.getHeight())));
            float fit = Math.min(1.0F, (float) ICON_SIDE / Math.max(source.getWidth(), source.getHeight()));
            int iconWidth = Math.max(1, Math.round(source.getWidth() * fit));
            int iconHeight = Math.max(1, Math.round(source.getHeight() * fit));
            return new Prepared(scale(source, width, height, width, height, 0, 0), scale(source, ICON_SIDE, ICON_SIDE, iconWidth, iconHeight,
                    (ICON_SIDE - iconWidth) / 2, (ICON_SIDE - iconHeight) / 2));
        } catch (ImageDecoder.DecodeException e) {
            throw new IllegalStateException(e.failure().name(), e);
        }
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
        entry.texture = register(client, "inline_art/" + id, prepared.glyph());
        entry.iconTexture = register(client, "inline_art/" + id + "_icon", prepared.icon());
        entry.glyph.show(entry.texture, prepared.glyph().width() / (float) SCALE);
        // The lines that hold the glyph were laid out before it had its width
        client.gui.hud.getChat().rescaleChat();
    }
}
