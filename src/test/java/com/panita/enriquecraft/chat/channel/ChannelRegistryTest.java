package com.panita.enriquecraft.chat.channel;

import net.minecraft.server.permissions.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelRegistryTest {

    private static ChannelDefinition channel(String id, List<String> aliases, boolean isDefault) {
        return new ChannelDefinition(id, aliases, "{message}", "", PermissionLevel.ALL, isDefault);
    }

    private final ChannelRegistry registry = new ChannelRegistry(() -> {
        Map<String, ChannelDefinition> channels = new LinkedHashMap<>();
        channels.put("global", channel("global", List.of("g"), true));
        channels.put("staff", channel("staff", List.of("s"), false));
        channels.put("trade", channel("trade", List.of("t"), false));
        return channels;
    });

    @Test
    void channelIsFoundByNameOrAliasWhateverTheCase() {
        assertEquals("staff", registry.find("staff").orElseThrow().id());
        assertEquals("staff", registry.find("S").orElseThrow().id());
        assertEquals("trade", registry.find("TRADE").orElseThrow().id());
    }

    @Test
    void unknownNameFindsNothing() {
        assertEquals(Optional.empty(), registry.find("nope"));
        assertEquals(Optional.empty(), registry.find(""));
    }

    @Test
    void theNamedAndDefaultChannelsAreTheOnesOfTheConfig() {
        assertEquals("global", registry.defaultChannel().id());
        assertEquals("global", registry.global().id());
        assertEquals("staff", registry.staff().id());
        assertEquals(3, registry.all().size());
    }

    @Test
    void aChannelAnswersWithAllTheWordsThatPickIt() {
        assertEquals(List.of("global", "g"), registry.global().names());
    }
}
