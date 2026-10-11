package com.panita.enriquecraft.chat.channel;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The channels of the config file. They are read when needed, so a reload is picked up. The config guarantees that
 * {@code global} and {@code staff} exist, that exactly one channel is the default and that no name is used twice.
 */
public final class ChannelRegistry {

    private final Supplier<Map<String, ChannelDefinition>> channels;

    public ChannelRegistry(Supplier<Map<String, ChannelDefinition>> channels) {
        this.channels = channels;
    }

    public Collection<ChannelDefinition> all() {
        return channels.get().values();
    }

    /** The channel a name or an alias stands for, whatever its case. */
    public Optional<ChannelDefinition> find(String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        return all().stream().filter(channel -> channel.names().contains(wanted)).findFirst();
    }

    public ChannelDefinition defaultChannel() {
        return all().stream().filter(ChannelDefinition::isDefault).findFirst().orElseThrow();
    }

    public ChannelDefinition global() {
        return channels.get().get(ChannelDefinition.GLOBAL);
    }

    public ChannelDefinition staff() {
        return channels.get().get(ChannelDefinition.STAFF);
    }
}
