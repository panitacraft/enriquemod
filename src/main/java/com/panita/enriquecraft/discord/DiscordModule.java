package com.panita.enriquecraft.discord;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.chat.service.KnownPlayers;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.io.SecretFile;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.message.DiscordView;
import com.panita.enriquecraft.discord.message.ImageView;
import com.panita.enriquecraft.discord.message.MediaView;
import com.panita.enriquecraft.discord.message.ShareView;
import com.panita.enriquecraft.discord.protocol.Protocol;
import com.panita.enriquecraft.discord.service.BridgeConnection;
import com.panita.enriquecraft.discord.service.DiscordDelivery;
import com.panita.enriquecraft.discord.service.ExecutorScheduler;
import com.panita.enriquecraft.discord.service.FrameRouter;
import com.panita.enriquecraft.discord.service.HttpMediaUploader;
import com.panita.enriquecraft.discord.service.ScreenshotShares;
import com.panita.enriquecraft.discord.service.ShareTransfers;
import com.panita.enriquecraft.discord.service.GameReporter;
import com.panita.enriquecraft.discord.service.ImageFetcher;
import com.panita.enriquecraft.discord.service.ImageMaps;
import com.panita.enriquecraft.discord.service.MediaPreviews;
import com.panita.enriquecraft.discord.service.MediaTokens;
import com.panita.enriquecraft.discord.service.PlayerInfos;
import com.panita.enriquecraft.discord.service.QueryAnswers;
import com.panita.enriquecraft.discord.service.ServerInfo;
import com.panita.enriquecraft.discord.service.ShownMaps;
import com.panita.enriquecraft.discord.transport.WebSocketTransport;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * The server's side of the Discord integration. The mod never talks to Discord: the Panita Bot does, and this module keeps
 * one connection to it, tells it what happens in the game and shows in the game what Discord members write. The game never
 * depends on the bot; without it, chat is simply not shared.
 */
public final class DiscordModule implements EnriquecraftModule {

    private static final String KEY_FILE = "enriquecraft-discord.key";
    private static final String KEY_VARIABLE = "ENRIQUECRAFT_BRIDGE_KEY";

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(DiscordMessages.class);
    }

    @Override
    public void registerServices(ServiceRegistry services) {
        DiscordConfig config = services.get(DiscordConfig.class);
        NicknameService nicknames = services.get(NicknameService.class);
        Path keyFile = FabricLoader.getInstance().getConfigDir().resolve(KEY_FILE);
        // Read at each attempt, so a key put in place later is found by the next one.
        Supplier<Optional<String>> key = () -> SecretFile.find(keyFile, KEY_VARIABLE, System::getenv);
        ServerInfo info = new ServerInfo(Instant::now,
                () -> config.mediaEnabled.get() ? List.of(Protocol.FEATURE_MEDIA) : List.of());
        FrameRouter router = new FrameRouter(info);
        BridgeConnection connection = new BridgeConnection(new WebSocketTransport(), new ExecutorScheduler(), Instant::now,
                () -> ThreadLocalRandom.current().nextDouble(), config.enabled::get, config.url::get, key, info::hello, router::route);

        MediaTokens tokens = new MediaTokens(Instant::now, Duration.ofMinutes(10), 500);
        ExecutorService worker = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "enriquecraft-images");
            thread.setDaemon(true);
            return thread;
        });
        ShownMaps shownMaps = new ShownMaps(Instant::now);
        DiscordHooks.install(shownMaps);
        ImageView imageView = new ImageView(services.get(Messenger.class));
        ImageMaps imageMaps = new ImageMaps(new ImageFetcher(worker, (int) Protocol.MAX_MEDIA_BYTES, Duration.ofSeconds(8)), shownMaps,
                imageView, config, Instant::now, worker);
        MediaView mediaView = new MediaView(services.get(MessageFormatter.class), tokens);
        MediaPreviews previews = new MediaPreviews(services.get(ClientCapabilities.class));
        DiscordDelivery delivery = new DiscordDelivery(services.get(ChatService.class), services.get(ChannelRegistry.class),
                services.get(MessageFormatter.class), config, mediaView, previews);
        PlayerInfos playerInfos = new PlayerInfos(nicknames);
        ScreenshotShares shares = new ScreenshotShares(new ShareTransfers(Instant::now), connection,
                new HttpMediaUploader(worker, config.url::get, key), worker, services.get(ChatService.class),
                services.get(ChannelSelection.class), services.get(HeldPlayers.class), playerInfos, mediaView, previews,
                new ShareView(services.get(Messenger.class)), config, services.get(ClientCapabilities.class), Instant::now);
        QueryAnswers answers = new QueryAnswers(connection, info, nicknames, services.get(HeldPlayers.class),
                services.get(KnownPlayers.class), Instant::now);
        router.onChat(delivery::deliver);
        router.onQuery(answers::answer);

        services.register(MediaTokens.class, tokens);
        services.register(ShownMaps.class, shownMaps);
        services.register(ImageView.class, imageView);
        services.register(ImageMaps.class, imageMaps);
        services.register(ServerInfo.class, info);
        services.register(BridgeConnection.class, connection);
        services.register(GameReporter.class, new GameReporter(connection));
        services.register(PlayerInfos.class, playerInfos);
        services.register(ScreenshotShares.class, shares);
        services.register(DiscordView.class, new DiscordView(services.get(Messenger.class)));
    }
}
