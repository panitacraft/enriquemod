package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.message.MediaView;
import com.panita.enriquecraft.discord.message.ShareView;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import com.panita.enriquecraft.discord.network.ShareChunkC2S;
import com.panita.enriquecraft.discord.network.ShareEndC2S;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import com.panita.enriquecraft.discord.protocol.Protocol;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Takes a screenshot a player chose to share from the client companion to Discord and the game chat: checks that the player and
 * the bot can do it, re-encodes the picture (on a worker), has the bot post it, and then shows it in the game chat. A
 * client sends nothing that decides where an image goes or what its address is: the address in the chat line is the one the bot
 * answered, and it must be on an allowed host like any other image. The request methods run on the server thread.
 */
public final class ScreenshotShares {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ShareTransfers transfers;
    private final BridgeConnection connection;
    private final MediaUploader uploader;
    private final Executor worker;
    private final ChatService chat;
    private final ChannelSelection selection;
    private final HeldPlayers held;
    private final PlayerInfos infos;
    private final MediaView media;
    private final MediaPreviews previews;
    private final ShareView view;
    private final DiscordConfig config;
    private final ClientCapabilities capabilities;
    private final Supplier<Instant> clock;
    private final Map<UUID, Deque<Instant>> recent = new HashMap<>();

    public ScreenshotShares(ShareTransfers transfers, BridgeConnection connection, MediaUploader uploader, Executor worker, ChatService chat,
                            ChannelSelection selection, HeldPlayers held, PlayerInfos infos, MediaView media, MediaPreviews previews,
                            ShareView view, DiscordConfig config, ClientCapabilities capabilities, Supplier<Instant> clock) {
        this.transfers = transfers;
        this.connection = connection;
        this.uploader = uploader;
        this.worker = worker;
        this.chat = chat;
        this.selection = selection;
        this.held = held;
        this.infos = infos;
        this.media = media;
        this.previews = previews;
        this.view = view;
        this.config = config;
        this.capabilities = capabilities;
        this.clock = clock;
    }

    public void begin(ServerPlayer player, ShareBeginC2S begin) {
        if (!allowed(player)) {
            return;
        }
        transfers.begin(player.getUUID(), begin).ifPresent(rejection -> {
            switch (rejection) {
                case BUSY -> view.busy(player);
                case TOO_LARGE -> view.tooLarge(player);
                case INVALID -> view.invalid(player);
            }
        });
    }

    public void chunk(ServerPlayer player, ShareChunkC2S chunk) {
        if (allowed(player)) {
            transfers.chunk(player.getUUID(), chunk);
        }
    }

    public void end(ServerPlayer player, ShareEndC2S end) {
        if (!allowed(player)) {
            return;
        }
        transfers.end(player.getUUID(), end).ifPresent(completed -> share(player, completed));
    }

    public void forget(UUID player) {
        transfers.forget(player);
        recent.remove(player);
    }

    /** Whether this player may be sending anything at all: the client announced it, and the player is in the game. */
    private boolean allowed(ServerPlayer player) {
        return capabilities.supports(player.getUUID(), ClientFeature.SCREENSHOT_SHARE) && !held.isHeld(player);
    }

    private void share(ServerPlayer player, ShareTransfers.Completed completed) {
        ChannelDefinition channel = selection.current(player);
        if (!config.shareEnabled.get() || !connection.offers(Protocol.FEATURE_MEDIA_UPLOAD) || !connection.bridges(channel.id())
                || !channel.canUse(player)) {
            view.unavailable(player);
            return;
        }
        if (!withinPace(player.getUUID())) {
            view.tooFast(player);
            return;
        }
        view.sending(player);
        MinecraftServer server = player.level().getServer();
        PlayerInfo info = infos.of(player);
        worker.execute(() -> {
            ScreenshotEncoder.Encoded encoded;
            try {
                encoded = ScreenshotEncoder.reencode(completed.bytes());
            } catch (ImageDecoder.DecodeException e) {
                server.execute(() -> view.failed(player, e));
                return;
            }
            uploader.upload(new MediaUploader.Request(info, channel.id(), completed.caption(), encoded.bytes()))
                    .whenComplete((uploaded, failure) -> server.execute(() -> {
                        if (failure != null) {
                            view.failed(player, failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure);
                        } else {
                            finish(server, player, channel, completed, uploaded);
                        }
                    }));
        });
    }

    private void finish(MinecraftServer server, ServerPlayer player, ChannelDefinition channel, ShareTransfers.Completed completed,
                        MediaUploader.Uploaded uploaded) {
        MediaEntry entry = new MediaEntry(uploaded.url(), DiscordMessages.Share.CHAT_NAME, uploaded.type(), uploaded.size(),
                OptionalInt.of(uploaded.width()), OptionalInt.of(uploaded.height()), false);
        if (!MediaLinks.allows(entry, MediaLinks.hostsOf(config.mediaHosts.get()))) {
            view.failed(player, new IllegalStateException("the bot answered an address on a host that is not allowed"));
            return;
        }
        List<ServerPlayer> readers = chat.deliverExternal(server, channel, player.getDisplayName(), completed.caption(), List.of(media.link(entry)));
        previews.send(readers, MediaLinks.isTrusted(entry) ? List.of(entry) : List.of());
    }

    /** Counts this share against the player's minute; false when they have used up what the setting allows. */
    private boolean withinPace(UUID player) {
        Instant now = clock.get();
        Deque<Instant> shares = recent.computeIfAbsent(player, id -> new ArrayDeque<>());
        while (!shares.isEmpty() && Duration.between(shares.peekFirst(), now).compareTo(WINDOW) > 0) {
            shares.removeFirst();
        }
        if (shares.size() >= config.sharePerMinute.get()) {
            return false;
        }
        shares.addLast(now);
        return true;
    }
}
