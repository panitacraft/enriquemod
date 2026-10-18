package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.ImageView;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
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
        Duration lifetime = Duration.ofSeconds(config.mediaMapSeconds.get());
        fetcher.fetch(URI.create(entry.url()))
                .thenApplyAsync(bytes -> pixels(bytes, dither), worker)
                .whenComplete((colors, error) -> server.execute(() -> {
                    loading.remove(id);
                    if (error != null) {
                        view.failed(player, error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                    } else if (!player.hasDisconnected()) {
                        maps.show(player, entry.name(), colors, lifetime);
                    }
                }));
    }

    private static byte[] pixels(byte[] bytes, boolean dither) {
        try {
            BufferedImage image = ImageDecoder.decode(bytes);
            return MapPixels.fit(image, dither);
        } catch (ImageDecoder.DecodeException e) {
            throw new CompletionException(e);
        }
    }
}
