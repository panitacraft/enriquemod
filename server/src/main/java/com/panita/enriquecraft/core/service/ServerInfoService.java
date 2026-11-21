package com.panita.enriquecraft.core.service;

import com.panita.enriquecraft.Enriquecraft;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;

/**
 * Collects the basic facts about the mod and the server it runs on.
 */
public final class ServerInfoService {

    private static final String LOADER_ID = "fabricloader";

    public ServerInfo collect(MinecraftServer server) {
        FabricLoader loader = FabricLoader.getInstance();
        ModContainer mod = loader.getModContainer(Enriquecraft.MOD_ID).orElseThrow();
        ModContainer fabricLoader = loader.getModContainer(LOADER_ID).orElseThrow();
        return new ServerInfo(
                mod.getMetadata().getName(),
                mod.getMetadata().getVersion().getFriendlyString(),
                server.getServerVersion(),
                fabricLoader.getMetadata().getVersion().getFriendlyString(),
                server.getPlayerCount(),
                server.getPlayerList().getMaxPlayers());
    }

    public record ServerInfo(String modName, String modVersion, String minecraftVersion, String loaderVersion,
                             int playerCount, int maxPlayers) {
    }
}
