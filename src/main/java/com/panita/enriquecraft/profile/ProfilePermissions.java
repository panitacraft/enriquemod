package com.panita.enriquecraft.profile;

import com.panita.enriquecraft.Enriquecraft;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/** What a player may do with their profile, asked of the permission manager. */
public final class ProfilePermissions {

    /** A prank level change that ignores the wait between changes. */
    public static final String PRANK_BYPASS = Enriquecraft.MOD_ID + ".profile.prank.bypass";

    private ProfilePermissions() {
    }

    public static boolean canBypassPrankCooldown(ServerPlayer player) {
        return Permissions.check(player, PRANK_BYPASS, PermissionLevel.ADMINS);
    }
}
