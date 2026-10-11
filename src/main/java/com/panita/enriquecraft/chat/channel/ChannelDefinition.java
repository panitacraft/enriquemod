package com.panita.enriquecraft.chat.channel;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.stream.Stream;

/**
 * A chat channel as the administrator described it: what its lines look like and who may read and write in it.
 *
 * @param id            the name of the channel
 * @param aliases       other names players can use for it
 * @param format        the text of a line, with the arguments {@code {sender}}, {@code {message}} and {@code {channel}}
 *                      and server placeholders
 * @param permission    the permission node needed to use it; empty when everyone may
 * @param fallbackLevel the vanilla level needed when no permission manager decides about the node
 * @param isDefault     whether players start out writing in it
 */
public record ChannelDefinition(String id, List<String> aliases, String format, String permission,
                                PermissionLevel fallbackLevel, boolean isDefault) {

    /** The channel everyone can write in; {@code /ac} sends to it. */
    public static final String GLOBAL = "global";
    /** The channel of the staff; {@code /sc} sends to it. */
    public static final String STAFF = "staff";

    public ChannelDefinition {
        aliases = List.copyOf(aliases);
    }

    /** Whether the player may read and write in the channel. */
    public boolean canUse(ServerPlayer player) {
        return permission.isEmpty() || Permissions.check(player, permission, fallbackLevel);
    }

    /** The name of the channel and its aliases: every word that picks it. */
    public List<String> names() {
        return Stream.concat(Stream.of(id), aliases.stream()).toList();
    }
}
