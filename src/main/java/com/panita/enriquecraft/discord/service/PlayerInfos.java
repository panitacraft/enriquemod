package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Describes a player to the bot: who they are, what they are called in chat, and whether their name belongs to a Microsoft
 * account, which picks the head the bot shows.
 */
public final class PlayerInfos {

    private final NicknameService nicknames;

    public PlayerInfos(NicknameService nicknames) {
        this.nicknames = nicknames;
    }

    public PlayerInfo of(ServerPlayer player) {
        UUID id = player.getUUID();
        String name = player.getGameProfile().name();
        return new PlayerInfo(id, name, nicknames.plainNicknameOf(id), AuthMode.of(id, name) == AuthMode.PREMIUM);
    }
}
