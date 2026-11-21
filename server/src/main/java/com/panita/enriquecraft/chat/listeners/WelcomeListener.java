package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.WelcomeView;
import com.panita.enriquecraft.core.framework.listener.ModListener;

/**
 * Welcomes a player once their login is done, not when they connect: until then they are only looking at the PIN screen.
 */
public final class WelcomeListener implements ModListener {

    private final ChatConfig config;
    private final WelcomeView view;

    public WelcomeListener(ChatConfig config, WelcomeView view) {
        this.config = config;
        this.view = view;
    }

    @Override
    public void register() {
        AuthEvents.PLAYER_READY.register(player -> {
            if (config.welcomeMessage.get()) {
                view.send(player);
            }
        });
    }
}
