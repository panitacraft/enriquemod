package com.panita.enriquecraft.discord;

import com.panita.enriquecraft.Enriquecraft;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/** What a player may do through the Discord bridge, asked of the permission manager. */
public final class DiscordPermissions {

    /** Mentioning a linked Discord member with {@code @name}, which pings them. */
    public static final String MENTION = Enriquecraft.MOD_ID + ".discord.mention";

    private DiscordPermissions() {
    }

    public static boolean canMention(ServerPlayer player) {
        return Permissions.check(player, MENTION, PermissionLevel.ADMINS);
    }
}
