package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.Protocol;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What the bridge knows about the running server: that it has started, when, and its icon. The hello is built from it, so
 * nothing is said to the bot before the server is up.
 */
public final class ServerInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfo.class);
    private static final String MOD_ID = "enriquecraft";
    private static final String SERVER_ICON = "server-icon.png";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final Supplier<Instant> clock;
    private final Supplier<List<String>> features;
    private volatile MinecraftServer server;
    private volatile Instant startedAt;
    private volatile Optional<byte[]> icon = Optional.empty();

    /** @param features the optional features to announce in the hello, read each time it is built */
    public ServerInfo(Supplier<Instant> clock, Supplier<List<String>> features) {
        this.clock = clock;
        this.features = features;
    }

    public void started(MinecraftServer started) {
        startedAt = clock.get();
        // Read once per run, not at every reconnect. The game's own copy (the status) is not built yet when this event fires.
        icon = fitIcon(readIcon(started.getFile(SERVER_ICON), started.getWorldScreenshotFile()));
        server = started;
    }

    /** The file the game shows in the server list: the one in the server folder, else the icon of the world. */
    static Optional<byte[]> readIcon(Path serverIcon, Optional<Path> worldIcon) {
        Optional<Path> file = Optional.of(serverIcon).filter(Files::isRegularFile).or(() -> worldIcon.filter(Files::isRegularFile));
        return file.flatMap(path -> {
            try {
                return Optional.of(Files.readAllBytes(path));
            } catch (IOException e) {
                LOGGER.warn("Could not read the server icon; it will not be shown in Discord", e);
                return Optional.empty();
            }
        });
    }

    /** The icon, if it is a PNG small enough for the hello; one that is not is left out and said once. */
    static Optional<byte[]> fitIcon(Optional<byte[]> candidate) {
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        byte[] bytes = candidate.get();
        if (bytes.length > Protocol.MAX_ICON_BYTES) {
            LOGGER.warn("The server icon has {} bytes and the bot only takes {}; it will not be shown in Discord", bytes.length,
                    Protocol.MAX_ICON_BYTES);
            return Optional.empty();
        }
        if (bytes.length < PNG_SIGNATURE.length || !Arrays.equals(bytes, 0, PNG_SIGNATURE.length, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)) {
            LOGGER.warn("The server icon is not a PNG; it will not be shown in Discord");
            return Optional.empty();
        }
        return candidate;
    }

    public void stopped() {
        server = null;
    }

    /** The running server, if there is one. */
    public Optional<MinecraftServer> server() {
        return Optional.ofNullable(server);
    }

    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<OutFrame.Hello> hello() {
        MinecraftServer running = server;
        if (running == null) {
            return Optional.empty();
        }
        String mod = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        return Optional.of(new OutFrame.Hello(Protocol.VERSION, mod, running.getServerVersion(), startedAt, running.getMaxPlayers(),
                icon, features.get()));
    }
}
