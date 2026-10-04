package com.panita.enriquecraft;

import com.mojang.serialization.DynamicOps;
import com.panita.enriquecraft.core.config.CoreConfig;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.channel.BossBarChannel;
import com.panita.enriquecraft.core.message.channel.TitleChannel;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;

/**
 * Starts Minecraft's registries once per test run, so tests can create items and components,
 * and builds the small object graph that menus need.
 */
public final class MinecraftTestSupport {

    private static boolean started;
    private static HolderLookup.Provider registries;

    private MinecraftTestSupport() {
    }

    public static synchronized void bootstrap() {
        if (!started) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            // Items only get their default components once server resources load; do the same here.
            registries = VanillaRegistries.createWorldLookup();
            HolderLookup.Provider lookup = VanillaRegistries.createReloadableLookup(registries);
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                    .forEach(DataComponentInitializers.PendingComponents::apply);
            started = true;
        }
    }

    /** How data is turned into JSON on a real server: with the registries that items need. */
    public static DynamicOps<Tag> ops() {
        bootstrap();
        return RegistryOps.create(NbtOps.INSTANCE, registries);
    }

    /** A message formatter backed by a default config stored in the given directory. */
    public static MessageFormatter formatter(Path configDirectory) {
        bootstrap();
        CoreConfig config = new ConfigManager(configDirectory).bind("core", CoreConfig.class);
        return new MessageFormatter(config);
    }

    public static MenuFactory menuFactory(Path configDirectory) {
        return new MenuFactory(formatter(configDirectory));
    }

    /** A buffer that can carry items and components, as the ones of a real connection can. */
    public static RegistryFriendlyByteBuf buffer() {
        bootstrap();
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    public static Messenger messenger(Path configDirectory) {
        return new Messenger(formatter(configDirectory), new TitleChannel(), new BossBarChannel());
    }
}
