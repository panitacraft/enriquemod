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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final int SCALE = 2;
    /** The height of a glyph in screen pixels, the height of the text. */
    private static final int HEIGHT = 8;
    private static final int MAX_WIDTH = 64;
    private static final int MAX_IMAGES = 256;
    private static final int MAX_BYTES = 1024 * 1024;

    /** What the client knows about the image of one glyph. */
    private static final class Entry {
        final InlineArtGlyph glyph = new InlineArtGlyph();
        boolean requested;
        Identifier texture;
    }

    private record Prepared(int width, int height, int[] pixels) {
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
        Minecraft client = Minecraft.getInstance();
        FETCHER.fetch(URI.create(url))
                .thenApplyAsync(InlineArtTextures::prepare, WORKER)
                .whenComplete((prepared, failure) -> client.execute(() -> {
                    if (failure != null) {
                        LOGGER.debug("The art {} could not be loaded", id, failure);
                    } else if (ENTRIES.get(id) == entry) {
                        show(client, id, entry, prepared);
                    }
                }));
    }

    /** Forgets everything, as the textures belong to the game session that is ending. */
    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        ENTRIES.values().stream().filter(entry -> entry.texture != null).forEach(entry -> client.getTextureManager().release(entry.texture));
        ENTRIES.values().forEach(entry -> entry.glyph.hide());
        ENTRIES.clear();
    }

    private static Entry entry(String id) {
        return ENTRIES.computeIfAbsent(id, key -> new Entry());
    }

    /** On the worker: decode and scale once to the height of a glyph, keeping the proportions of the image. */
    private static Prepared prepare(byte[] bytes) {
        try {
            BufferedImage source = ImageDecoder.decode(bytes);
            int height = HEIGHT * SCALE;
            int width = Math.max(1, Math.min(MAX_WIDTH * SCALE, Math.round(source.getWidth() * (float) height / source.getHeight())));
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = scaled.createGraphics();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                canvas.drawImage(source, 0, 0, width, height, null);
            } finally {
                canvas.dispose();
            }
            return new Prepared(width, height, scaled.getRGB(0, 0, width, height, null, 0, width));
        } catch (ImageDecoder.DecodeException e) {
            throw new IllegalStateException(e.failure().name(), e);
        }
    }

    /** On the client thread: the texture is made here, and the glyph starts drawing it. */
    private static void show(Minecraft client, String id, Entry entry, Prepared prepared) {
        NativeImage pixels = new NativeImage(prepared.width(), prepared.height(), true);
        for (int y = 0; y < prepared.height(); y++) {
            for (int x = 0; x < prepared.width(); x++) {
                pixels.setPixel(x, y, prepared.pixels()[y * prepared.width() + x]);
            }
        }
        Identifier texture = Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, "inline_art/" + id);
        client.getTextureManager().register(texture, new DynamicTexture(() -> "enriquecraft inline art", pixels));
        entry.texture = texture;
        entry.glyph.show(texture, prepared.width() / (float) SCALE);
        // The lines that hold the glyph were laid out before it had its width
        client.gui.hud.getChat().rescaleChat();
    }
}
