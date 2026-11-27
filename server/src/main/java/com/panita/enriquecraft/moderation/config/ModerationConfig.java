package com.panita.enriquecraft.moderation.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Settings of the moderation module, stored in the {@code moderation} section of the config file.
 */
public final class ModerationConfig implements ModConfig {

    public final ConfigValue<Boolean> clientModsEnabled;
    public final ConfigValue<Map<String, String>> blockedChannels;

    public final ConfigValue<String> voiceChatGroup;
    public final ConfigValue<String> voiceChatSpeakPermission;

    public ModerationConfig(ConfigSectionBuilder builder) {
        voiceChatGroup = builder.string("voiceChat.group", "default",
                "The LuckPerms group everybody is in. /enriquecraft voicechat mute all denies the speaking permission to this group.",
                value -> value.matches("[A-Za-z0-9_.-]{1,64}"), "must be 1 to 64 letters, digits, ., - or _");
        voiceChatSpeakPermission = builder.string("voiceChat.speakPermission", "voicechat.speak",
                "The permission the voice chat mod checks before it lets a player speak. /enriquecraft voicechat mute and unmute set it with\n"
                        + "LuckPerms: false to silence, removed to free. Muting everyone grants it to the players of auth.staffOnly.players.",
                value -> value.matches("[A-Za-z0-9_.-]{1,128}"), "must be 1 to 128 letters, digits, ., - or _");
        clientModsEnabled = builder.bool("clientMods.enabled", true,
                "Kicks players whose client announces one of the channels below. The server cannot read a client's mods; it can only see\n"
                        + "the channels the client says it listens on, so a client that does not announce them is not caught.");
        blockedChannels = builder.stringMap("clientMods.blockedChannels", Map.of("worldinfo:world_id", "JourneyMap o VoxelMap"),
                "The channels that get a player kicked, each with the name shown to the player. JourneyMap and VoxelMap both ask the server\n"
                        + "for the id of the world on worldinfo:world_id; Xaero's mods use channels of their own, so they are not affected.",
                key -> Identifier.tryParse(key) != null, "must be a channel id such as namespace:path");
    }
}
