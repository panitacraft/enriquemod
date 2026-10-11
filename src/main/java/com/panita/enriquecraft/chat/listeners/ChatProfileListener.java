package com.panita.enriquecraft.chat.listeners;

import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Remembers the name of every player who joins, so a nickname can never be the name of someone who is away.
 */
public final class ChatProfileListener implements ModListener {

    private final NicknameService nicknames;

    public ChatProfileListener(NicknameService nicknames) {
        this.nicknames = nicknames;
    }

    @Override
    public void register() {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> nicknames.touch(handler.getPlayer().getUUID(), handler.getPlayer().getGameProfile().name()));
    }
}
