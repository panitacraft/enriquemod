package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.Protocol;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What the bridge knows about the running server: that it has started, when, and its icon. The hello is built from it, so
 * nothing is said to the bot before the server is up.
 */
public final class ServerInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfo.class);
    private static final String MOD_ID = "enriquecraft";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final Supplier<Instant> clock;
    private volatile MinecraftServer server;
    private volatile Instant startedAt;
    private volatile Optional<byte[]> icon = Optional.empty();

    public ServerInfo(Supplier<Instant> clock) {
        this.clock = clock;
    }

    public void started(MinecraftServer started) {
        startedAt = clock.get();
        // The game has already read the icon, and checked that it is a 64x64 PNG, to answer the server list; its status is built
        // before this event, so it is there. Read once per run, not at every reconnect.
        icon = fitIcon(Optional.ofNullable(started.getStatus()).flatMap(ServerStatus::favicon).map(ServerStatus.Favicon::iconBytes));
        server = started;
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
                icon));
    }
}
