package com.panita.enriquecraft.discord;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.chat.service.KnownPlayers;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.io.SecretFile;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.message.DiscordView;
import com.panita.enriquecraft.discord.service.BridgeConnection;
import com.panita.enriquecraft.discord.service.DiscordDelivery;
import com.panita.enriquecraft.discord.service.ExecutorScheduler;
import com.panita.enriquecraft.discord.service.FrameRouter;
import com.panita.enriquecraft.discord.service.GameReporter;
import com.panita.enriquecraft.discord.service.PlayerInfos;
import com.panita.enriquecraft.discord.service.QueryAnswers;
import com.panita.enriquecraft.discord.service.ServerInfo;
import com.panita.enriquecraft.discord.transport.WebSocketTransport;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
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
        ServerInfo info = new ServerInfo(Instant::now);
        FrameRouter router = new FrameRouter(info);
        BridgeConnection connection = new BridgeConnection(new WebSocketTransport(), new ExecutorScheduler(), Instant::now,
                () -> ThreadLocalRandom.current().nextDouble(), config.enabled::get, config.url::get, key, info::hello, router::route);

        DiscordDelivery delivery = new DiscordDelivery(services.get(ChatService.class), services.get(ChannelRegistry.class),
                services.get(MessageFormatter.class), config);
        QueryAnswers answers = new QueryAnswers(connection, info, nicknames, services.get(HeldPlayers.class),
                services.get(KnownPlayers.class), Instant::now);
        router.onChat(delivery::deliver);
        router.onQuery(answers::answer);

        services.register(ServerInfo.class, info);
        services.register(BridgeConnection.class, connection);
        services.register(GameReporter.class, new GameReporter(connection));
        services.register(PlayerInfos.class, new PlayerInfos(nicknames));
        services.register(DiscordView.class, new DiscordView(services.get(Messenger.class)));
    }
}
