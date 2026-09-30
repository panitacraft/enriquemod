package com.panita.enriquecraft;

import com.panita.enriquecraft.core.config.CoreConfig;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;

/**
 * Starts Minecraft's registries once per test run, so tests can create items and components,
 * and builds the small object graph that menus need.
 */
public final class MinecraftTestSupport {

    private static boolean started;

    private MinecraftTestSupport() {
    }

    public static synchronized void bootstrap() {
        if (!started) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            // Items only get their default components once server resources load; do the same here.
            HolderLookup.Provider lookup = VanillaRegistries.createReloadableLookup(VanillaRegistries.createWorldLookup());
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                    .forEach(DataComponentInitializers.PendingComponents::apply);
            started = true;
        }
    }

    /** A menu factory backed by a default config stored in the given directory. */
    public static MenuFactory menuFactory(Path configDirectory) {
        bootstrap();
        CoreConfig config = new ConfigManager(configDirectory).bind("core", CoreConfig.class);
        return new MenuFactory(new MessageFormatter(config));
    }
}
