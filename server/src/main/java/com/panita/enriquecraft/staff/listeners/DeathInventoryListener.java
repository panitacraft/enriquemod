package com.panita.enriquecraft.staff.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Records a player's inventory the moment before vanilla drops it. It only reads: the death
 * always goes ahead.
 */
public final class DeathInventoryListener implements ModListener {

    private final DeathInventoryService service;

    public DeathInventoryListener(DeathInventoryService service) {
        this.service = service;
    }

    @Override
    public void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                service.capture(player, source);
            }
            return true;
        });
    }
}
