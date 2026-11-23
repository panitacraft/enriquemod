package com.panita.enriquecraft.qol;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.moderation.service.MuteService;
import com.panita.enriquecraft.qol.config.QolConfig;
import com.panita.enriquecraft.qol.message.CoordinateShareView;
import com.panita.enriquecraft.qol.message.QolMessages;
import com.panita.enriquecraft.qol.service.CoordinateSharing;
import com.panita.enriquecraft.qol.service.ShareSounds;

import java.util.Optional;

/**
 * Small conveniences for players. For now: sharing coordinates with everyone or with one player.
 */
public final class QolModule implements EnriquecraftModule {

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(QolMessages.class);
    }

    @Override
    public void registerServices(ServiceRegistry services) {
        services.register(CoordinateSharing.class, new CoordinateSharing(services.get(QolConfig.class),
                services.get(HeldPlayers.class), services.get(MuteService.class),
                new CoordinateShareView(services.get(Messenger.class)), new ShareSounds()));
    }
}
