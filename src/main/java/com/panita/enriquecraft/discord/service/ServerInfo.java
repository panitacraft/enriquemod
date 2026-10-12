package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.Protocol;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What the bridge knows about the running server: that it has started, and when. The hello is built from it, so nothing
 * is said to the bot before the server is up.
 */
public final class ServerInfo {

    private static final String MOD_ID = "enriquecraft";

    private final Supplier<Instant> clock;
    private volatile MinecraftServer server;
    private volatile Instant startedAt;

    public ServerInfo(Supplier<Instant> clock) {
        this.clock = clock;
    }

    public void started(MinecraftServer started) {
        startedAt = clock.get();
        server = started;
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
        return Optional.of(new OutFrame.Hello(Protocol.VERSION, mod, running.getServerVersion(), startedAt, running.getMaxPlayers()));
    }
}
