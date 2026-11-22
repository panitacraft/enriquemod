package com.panita.enriquecraft.moderation.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.moderation.message.ClientModView;
import com.panita.enriquecraft.moderation.service.ClientModPolicy;
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kicks a player as soon as their client announces a channel of a mod that is not allowed. Fabric reports the channels a client
 * listens on right after it joins.
 */
public final class ClientModListener implements ModListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientModListener.class);

    private final ClientModPolicy policy;
    private final ClientModView view;

    public ClientModListener(ClientModPolicy policy, ClientModView view) {
        this.policy = policy;
        this.view = view;
    }

    @Override
    public void register() {
        ClientboundPlayChannelEvents.REGISTER.register((handler, sender, server, channels) ->
                policy.blockedMod(channels).ifPresent(mod -> server.execute(() -> {
                    LOGGER.info("Kicking {}: the client announced a channel of {}", handler.getPlayer().getGameProfile().name(), mod);
                    handler.disconnect(view.screen(mod));
                })));
    }
}
