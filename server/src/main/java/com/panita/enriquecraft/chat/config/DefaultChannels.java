package com.panita.enriquecraft.chat.config;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.message.ChatMessages;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The channels a new config file starts with: the one everybody uses and the one of the staff. */
final class DefaultChannels {

    private DefaultChannels() {
    }

    static Map<String, ChannelDefinition> table() {
        Map<String, ChannelDefinition> channels = new LinkedHashMap<>();
        channels.put(ChannelDefinition.GLOBAL, new ChannelDefinition(ChannelDefinition.GLOBAL, List.of("g"),
                ChatMessages.Format.GLOBAL, "", PermissionLevel.ALL, true));
        channels.put(ChannelDefinition.STAFF, new ChannelDefinition(ChannelDefinition.STAFF, List.of("s"),
                ChatMessages.Format.STAFF, "enriquecraft.chat.channel.staff", PermissionLevel.GAMEMASTERS, false));
        return Collections.unmodifiableMap(channels);
    }
}
