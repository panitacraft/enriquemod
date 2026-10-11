package com.panita.enriquecraft.chat.config;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

import java.util.Map;

/**
 * Settings of the chat module, stored in the {@code chat} section of the config file.
 */
public final class ChatConfig implements ModConfig {

    public final ConfigValue<Map<String, ChannelDefinition>> channels;
    public final ConfigValue<Map<String, String>> emojis;
    public final ConfigValue<Integer> nicknameMinLength;
    public final ConfigValue<Integer> nicknameMaxLength;
    public final ConfigValue<Integer> nicknameCooldownSeconds;
    public final ConfigValue<Boolean> joinMessage;
    public final ConfigValue<Boolean> firstJoinMessage;
    public final ConfigValue<Boolean> leaveMessage;
    public final ConfigValue<Boolean> deathMessage;
    public final ConfigValue<Boolean> advancementMessage;
    public final ConfigValue<Boolean> welcomeMessage;

    public ChatConfig(ConfigSectionBuilder builder) {
        channels = builder.custom("channels", DefaultChannels.table(),
                "The chat channels. A player listens to every channel they may use and writes in one, chosen with /chat.\n"
                        + "aliases: other names for the channel. format: a line of chat, where {sender}, {message} and {channel} are filled in\n"
                        + "and placeholders such as %luckperms:prefix% work. permission: the node needed, empty for everyone.\n"
                        + "fallbackLevel: the vanilla level needed when no permission manager decides (0 everyone, 2 game masters, 4 owners).\n"
                        + "default: the channel players start in. global and staff must exist: /ac and /sc send to them.",
                new ChannelsType());
        emojis = builder.stringMap("emojis", DefaultEmojis.table(),
                "Emojis players can write: the text they type and what it turns into. What it turns into accepts text tags.\n"
                        + "Use symbols the game's font draws. Remove every entry to turn emojis off.",
                trigger -> !trigger.isBlank() && trigger.length() <= 32 && trigger.strip().equals(trigger) && !trigger.contains(" "),
                "must be 1 to 32 characters long, without spaces");
        nicknameMinLength = builder.intRange("nickname.minLength", 3, 1, 32,
                "The fewest characters a nickname can have, not counting its colors and styles.");
        nicknameMaxLength = builder.intRange("nickname.maxLength", 16, 1, 32,
                "The most characters a nickname can have, not counting its colors and styles.");
        nicknameCooldownSeconds = builder.intRange("nickname.cooldownSeconds", 300, 0, 86_400,
                "Seconds a player must wait before changing their nickname again. 0 turns the wait off.");
        joinMessage = builder.bool("events.join.enabled", true, "Shows a message in chat when a player joins.");
        firstJoinMessage = builder.bool("events.firstJoin.enabled", true,
                "Shows a message of its own when a player joins for the first time. When it is off that join gets the normal message.");
        leaveMessage = builder.bool("events.leave.enabled", true, "Shows a message in chat when a player leaves.");
        deathMessage = builder.bool("events.death.enabled", true, "Shows a message in chat when a player dies.");
        advancementMessage = builder.bool("events.advancement.enabled", true, "Shows a message in chat when a player makes an advancement.");
        welcomeMessage = builder.bool("welcome.enabled", true, "Sends the welcome message to a player once they are in.");
    }
}
