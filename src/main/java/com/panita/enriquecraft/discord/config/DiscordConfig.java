package com.panita.enriquecraft.discord.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

import java.net.URI;
import java.util.Set;

/**
 * Settings of the discord module, stored in the {@code discord} section of the config file. The key of the bridge is
 * not here: it is a secret and lives in its own file.
 */
public final class DiscordConfig implements ModConfig {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    public final ConfigValue<Boolean> enabled;
    public final ConfigValue<String> url;
    public final ConfigValue<String> deliveryTag;

    public DiscordConfig(ConfigSectionBuilder builder) {
        enabled = builder.bool("enabled", true,
                "Whether the server connects to the Panita Bot to share chat and events with Discord.");
        url = builder.string("url", "",
                "The address of the bridge of the Panita Bot, such as \"wss://bot.example.com/bridge\". Empty keeps the connection off.",
                DiscordConfig::isBridgeUrl, "must be empty, or a wss address (ws is only allowed for localhost)");
        deliveryTag = builder.text("deliveryTag", "Discord",
                "The tag shown before the name of a Discord member in the game chat.");
    }

    /** Whether the address may be the bridge's: secure, or a local one for development. */
    static boolean isBridgeUrl(String value) {
        if (value.isEmpty()) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null) {
                return false;
            }
            return "wss".equals(uri.getScheme()) || ("ws".equals(uri.getScheme()) && LOCAL_HOSTS.contains(uri.getHost()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
