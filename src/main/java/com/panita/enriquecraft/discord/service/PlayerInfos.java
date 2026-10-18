package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Describes a player to the bot: who they are, what they are called in chat, whether their name belongs to a Microsoft
 * account and which skin they wear, which pick the head the bot shows.
 */
public final class PlayerInfos {

    private final NicknameService nicknames;

    public PlayerInfos(NicknameService nicknames) {
        this.nicknames = nicknames;
    }

    /** A player who is not online: no skin is known, so the bot draws the head of their account. */
    public PlayerInfo offline(UUID id, String name) {
        return new PlayerInfo(id, name, nicknames.plainNicknameOf(id), AuthMode.of(id, name) == AuthMode.PREMIUM, Optional.empty());
    }

    public PlayerInfo of(ServerPlayer player) {
        UUID id = player.getUUID();
        String name = player.getGameProfile().name();
        return new PlayerInfo(id, name, nicknames.plainNicknameOf(id), AuthMode.of(id, name) == AuthMode.PREMIUM,
                // Read at each use: SkinsRestorer replaces the profile of the player when they change their skin
                SkinTextures.of(player.getGameProfile()));
    }
}
