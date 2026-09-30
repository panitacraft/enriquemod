package com.panita.enriquecraft.core.listeners;

import com.mojang.serialization.JsonOps;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Loads the world's data files when the world starts and lets go of them when it stops.
 */
public final class WorldDataListener implements ModListener {

    private static final String DIRECTORY = "enriquecraft";

    private final WorldData worldData;

    public WorldDataListener(WorldData worldData) {
        this.worldData = worldData;
    }

    @Override
    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::attach);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> worldData.detach());
    }

    private void attach(MinecraftServer server) {
        worldData.attach(server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY),
                RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()));
    }
}
