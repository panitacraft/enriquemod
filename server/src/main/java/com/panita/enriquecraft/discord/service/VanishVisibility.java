package com.panita.enriquecraft.discord.service;

import me.drex.vanish.api.VanishAPI;
import me.drex.vanish.api.VanishEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * The link to the Vanish mod, which a server may or may not have: it is compile-only, so this class must only be loaded after
 * {@link #MOD_ID} was found to be installed. Everything that touches its API is here and nowhere else.
 */
public final class VanishVisibility implements PlayerVisibility {

    /** The mod id of Vanish; it also provides the older id {@code melius-vanish}. */
    public static final String MOD_ID = "vanish";

    @Override
    public boolean isHidden(ServerPlayer player) {
        return VanishAPI.isVanished(player);
    }

    /**
     * Follows the players who vanish and appear while they are online: a player who vanishes is reported as having left and one who
     * appears as having joined, as the fake messages of the Vanish mod say in the game. A player who joins already vanished is never
     * reported, since {@link GameReporter#ready} is asked only for players who are not hidden.
     */
    public static void follow(GameReporter reporter, PlayerInfos infos) {
        VanishEvents.VANISH_EVENT.register((player, vanished) -> {
            if (vanished) {
                reporter.left(infos.of(player));
            } else {
                reporter.revealed(infos.of(player));
            }
        });
    }
}
