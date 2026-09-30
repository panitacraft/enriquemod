package com.panita.enriquecraft.core.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;
import com.panita.enriquecraft.core.message.Messages;

/**
 * Settings of the core module, stored in the {@code core} section of the config file.
 */
public final class CoreConfig implements ModConfig {

    public final ConfigValue<String> prefix;
    public final ConfigValue<String> broadcastFormat;

    public CoreConfig(ConfigSectionBuilder builder) {
        prefix = builder.string("messages.prefix", Messages.Prefix.DEFAULT,
                "Text shown before every message of the mod. Supports text tags.",
                text -> !text.isBlank(), "must not be blank");
        broadcastFormat = builder.string("broadcast.format", Messages.Broadcast.FORMAT,
                "Layout of /broadcast. Write {message} where the announcement text goes. Supports text tags.",
                format -> format.contains("{message}"), "must contain {message}");
    }
}
