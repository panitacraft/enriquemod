package com.panita.enriquecraft.chat.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

import java.util.Map;

/**
 * Settings of the chat module, stored in the {@code chat} section of the config file.
 */
public final class ChatConfig implements ModConfig {

    public final ConfigValue<Map<String, String>> emojis;

    public ChatConfig(ConfigSectionBuilder builder) {
        emojis = builder.stringMap("emojis", DefaultEmojis.table(),
                "Emojis players can write: the text they type and what it turns into. What it turns into accepts text tags.\n"
                        + "Use symbols the game's font draws. Remove every entry to turn emojis off.",
                trigger -> !trigger.isBlank() && trigger.length() <= 32 && trigger.strip().equals(trigger) && !trigger.contains(" "),
                "must be 1 to 32 characters long, without spaces");
    }
}
