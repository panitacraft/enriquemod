package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.service.GameReporter;
import com.panita.enriquecraft.discord.service.PlayerInfos;
import com.panita.enriquecraft.discord.service.VanishVisibility;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Tells the bot when a player vanishes and appears, if the Vanish mod is installed. Without it nothing is followed and the Vanish
 * classes are never loaded.
 */
public final class VanishListener implements ModListener {

    private final GameReporter reporter;
    private final PlayerInfos infos;

    public VanishListener(GameReporter reporter, PlayerInfos infos) {
        this.reporter = reporter;
        this.infos = infos;
    }

    @Override
    public void register() {
        if (FabricLoader.getInstance().isModLoaded(VanishVisibility.MOD_ID)) {
            VanishVisibility.follow(reporter, infos);
        }
    }
}
