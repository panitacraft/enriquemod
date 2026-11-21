package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.ImageView;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * What happens when a player clicks the label of an image: the image is downloaded and turned into map colors on a worker
 * and then shown on a map in their hand. A player has one request at a time and waits a few seconds between requests. Runs
 * on the server thread, except the work it hands to the worker.
 */
public final class ImageMaps {

    private final ImageFetcher fetcher;
    private final ShownMaps maps;
    private final ImageView view;
    private final DiscordConfig config;
    private final Supplier<Instant> clock;
    private final Executor worker;
    private final Set<UUID> loading = new HashSet<>();
    private final Map<UUID, Instant> lastRequest = new HashMap<>();

    public ImageMaps(ImageFetcher fetcher, ShownMaps maps, ImageView view, DiscordConfig config, Supplier<Instant> clock, Executor worker) {
        this.fetcher = fetcher;
        this.maps = maps;
        this.view = view;
        this.config = config;
        this.clock = clock;
        this.worker = worker;
    }

    public void show(MinecraftServer server, ServerPlayer player, MediaEntry entry) {
        if (!MediaLinks.allows(entry, MediaLinks.hostsOf(config.mediaHosts.get()))) {
            view.unavailable(player);
            return;
        }
        UUID id = player.getUUID();
        if (loading.contains(id)) {
            view.busy(player);
            return;
        }
        Instant now = clock.get();
        Duration wait = Duration.ofSeconds(config.mediaCooldownSeconds.get());
        Instant last = lastRequest.get(id);
        if (last != null && last.plus(wait).isAfter(now)) {
            view.cooldown(player);
            return;
        }
        lastRequest.put(id, now);
        loading.add(id);
        view.showing(player);
        boolean dither = config.mediaDither.get();
        boolean animate = config.mediaAnimate.get();
        Duration lifetime = Duration.ofSeconds(config.mediaMapSeconds.get());
        fetcher.fetch(URI.create(entry.url()))
                .thenApplyAsync(bytes -> animation(bytes, dither, animate), worker)
                .whenComplete((colors, error) -> server.execute(() -> {
                    loading.remove(id);
                    if (error != null) {
                        view.failed(player, error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                    } else if (!player.hasDisconnected()) {
                        maps.show(player, entry.name(), colors, lifetime);
                    }
                }));
    }

    private static final int MIN_FRAME_MS = 50;
    /** A GIF that asks for no delay, or one too short to see, is played at the pace browsers use. */
    private static final int DEFAULT_FRAME_MS = 100;

    private static MapAnimation animation(byte[] bytes, boolean dither, boolean animate) {
        try {
            if (animate && isGif(bytes)) {
                List<byte[]> frames = new ArrayList<>();
                List<Integer> delays = new ArrayList<>();
                // The picture changes from frame to frame, so blending the error would make it shimmer: no dithering here
                GifDecoder.frames(bytes, GifDecoder.MAX_FRAMES, (canvas, delay) -> {
                    frames.add(MapPixels.fit(canvas, false));
                    delays.add(delay <= 10 ? DEFAULT_FRAME_MS : Math.max(delay, MIN_FRAME_MS));
                });
                if (frames.size() > 1) {
                    return new MapAnimation(frames, delays);
                }
            }
            return MapAnimation.still(MapPixels.fit(ImageDecoder.decode(bytes), dither));
        } catch (ImageDecoder.DecodeException e) {
            throw new CompletionException(e);
        }
    }

    private static boolean isGif(byte[] bytes) {
        return bytes.length > 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8';
    }
}
